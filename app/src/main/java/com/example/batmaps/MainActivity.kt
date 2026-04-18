package com.example.batmaps

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.batmaps.ui.theme.BatMapsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray

data class Segnalazione(
    val data: String,
    val specie: String,
    val localita: String,
    val comune: String,
    val stato: String,
    val note: String,
    val latitude: Double,
    val longitude: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BatMapsApp/18.20"
        enableEdgeToEdge()
        setContent { BatMapsTheme { BatMapScreen() } }
    }
}

suspend fun getCoordinatesFromNominatim(queryText: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    if (queryText.isBlank()) return@withContext null
    return@withContext try {
        val query = URLEncoder.encode(queryText, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=1&email=marcello.consolo@gmail.com")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "BatMapsApp/18.20")
        conn.connectTimeout = 5000
        
        val response = conn.inputStream.bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(response)
        if (jsonArray.length() > 0) {
            val first = jsonArray.getJSONObject(0)
            Pair(first.getDouble("lat"), first.getDouble("lon"))
        } else null
    } catch (e: Exception) {
        Log.e("BatMaps", "Errore Nominatim per $queryText: ${e.message}")
        null
    }
}

@Composable
fun BatMapScreen() {
    val context = LocalContext.current
    val tutteLeSegnalazioni = remember { mutableStateListOf<Pair<Segnalazione, GeoPoint>>() }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("Inizializzazione...") }
    
    LaunchedEffect(Unit) {
        ComuniDatabase.initialize(context)
        leggiExcelIncrementale(context, 
            onProgress = { statusMessage = it },
            onNewPoint = { tutteLeSegnalazioni.add(it) },
            onComplete = { isLoading = false }
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OSMMapView(tutteLeSegnalazioni.toList())

            if (isLoading || tutteLeSegnalazioni.isEmpty()) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth(0.8f),
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(statusMessage, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        if (tutteLeSegnalazioni.isNotEmpty()) {
                            Text("Trovati: ${tutteLeSegnalazioni.size}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                color = Color.White.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2c3e50))
            ) {
                Text(
                    text = "BatMaps 2025 - v18.20",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF2c3e50),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OSMMapView(punti: List<Pair<Segnalazione, GeoPoint>>) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(6.0)
            controller.setCenter(GeoPoint(41.9, 12.5))
        }
    }

    LaunchedEffect(punti.size) {
        mapView.overlays.clear()
        punti.forEach { (info, coordinata) ->
            val marker = Marker(mapView)
            marker.position = coordinata
            marker.title = info.specie
            val color = when {
                info.stato.lowercase().contains("liberato") -> android.graphics.Color.GREEN
                info.stato.lowercase().contains("morto") -> android.graphics.Color.RED
                info.stato.lowercase().contains("degenza") -> android.graphics.Color.YELLOW
                else -> android.graphics.Color.BLUE
            }
            marker.icon.mutate().setTint(color)
            marker.snippet = "Data: ${info.data}\nLoc: ${info.localita}\nCom: ${info.comune}\nNote: ${info.note}"
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }
    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

suspend fun leggiExcelIncrementale(
    context: Context, 
    onProgress: (String) -> Unit, 
    onNewPoint: (Pair<Segnalazione, GeoPoint>) -> Unit,
    onComplete: () -> Unit
) = withContext(Dispatchers.IO) {
    val formatter = DataFormatter()
    try {
        val inputStream: InputStream = context.assets.open("Pipistrelli 2025.xlsx")
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)
        
        var headerIdx = -1
        for (i in 0..20) {
            val r = sheet.getRow(i) ?: continue
            val firstCell = formatter.formatCellValue(r.getCell(0)).lowercase()
            if (firstCell.contains("specie") || firstCell.contains("data")) { headerIdx = i; break }
        }
        
        if (headerIdx == -1) {
            withContext(Dispatchers.Main) { onProgress("Errore: Intestazione Excel non trovata") }
            return@withContext
        }

        val headerRow = sheet.getRow(headerIdx)
        val colMap = mutableMapOf<String, Int>()
        for (j in 0 until headerRow.lastCellNum.toInt()) {
            val name = formatter.formatCellValue(headerRow.getCell(j)).lowercase()
            if (name.contains("specie")) colMap["specie"] = j
            if (name.contains("data")) colMap["data"] = j
            if (name.contains("localit")) colMap["loc"] = j
            if (name.contains("comune")) colMap["comune"] = j
            if (name.contains("stato")) colMap["stato"] = j
            if (name.contains("note") || name.contains("condizioni")) colMap["note"] = j
        }

        for (i in (headerIdx + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val locRaw = formatter.formatCellValue(row.getCell(colMap["loc"] ?: -1)).trim()
            val comRaw = formatter.formatCellValue(row.getCell(colMap["comune"] ?: -1)).trim()
            if (locRaw.isBlank() && comRaw.isBlank()) continue

            val query = if (locRaw.isNotBlank() && locRaw.lowercase() != comRaw.lowercase()) "$locRaw, $comRaw" else comRaw
            withContext(Dispatchers.Main) { onProgress("Geocodifica: $query") }

            var coords = getCoordinatesFromNominatim(query)
            if (coords == null && locRaw.isNotBlank()) {
                coords = getCoordinatesFromNominatim(comRaw)
            }
            
            if (coords != null) {
                val dataStr = formatter.formatCellValue(row.getCell(colMap["data"] ?: -1))
                val point = Pair(
                    Segnalazione(
                        dataStr,
                        formatter.formatCellValue(row.getCell(colMap["specie"] ?: -1)),
                        locRaw, comRaw,
                        formatter.formatCellValue(row.getCell(colMap["stato"] ?: -1)),
                        formatter.formatCellValue(row.getCell(colMap["note"] ?: -1)),
                        coords.first, coords.second
                    ),
                    GeoPoint(coords.first, coords.second)
                )
                withContext(Dispatchers.Main) { onNewPoint(point) }
                delay(1200) 
            }
        }
        workbook.close()
    } catch (e: Exception) { 
        Log.e("BatMaps", "Errore critico: ${e.message}") 
    }
    withContext(Dispatchers.Main) { onComplete() }
}
