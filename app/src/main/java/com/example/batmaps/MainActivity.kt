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
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.InputStream

data class Segnalazione(
    val data: String,
    val specie: String,
    val localita: String,
    val comune: String,
    val provincia: String,
    val stato: String,
    val note: String,
    var latitude: Double,
    var longitude: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        setContent { BatMapsTheme { BatMapScreen() } }
    }
}

@Composable
fun BatMapScreen() {
    val context = LocalContext.current
    var segnalazioni by remember { mutableStateOf<List<Pair<Segnalazione, GeoPoint>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        segnalazioni = withContext(Dispatchers.IO) { leggiExcel(context) }
        isLoading = false
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                OSMMapView(segnalazioni)
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        .background(Color.White.copy(alpha = 0.8f), MaterialTheme.shapes.medium).padding(8.dp)
                ) {
                    Text("Totale: ${segnalazioni.size}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
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
            controller.setZoom(9.0)
            controller.setCenter(GeoPoint(45.5479, 11.5446))
        }
    }

    LaunchedEffect(punti) {
        mapView.overlays.clear()
        punti.forEach { (info, coordinata) ->
            val marker = Marker(mapView)
            marker.position = coordinata
            marker.title = info.specie
            marker.snippet = "${info.data}\n\n" +
                             "Loc: ${info.localita}\n" +
                             "Com: ${info.comune}\n" +
                             "Prov: ${info.provincia}\n" +
                             "Stato: ${info.stato}\n" +
                             "Note: ${info.note}"
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }
    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

fun leggiExcel(context: Context): List<Pair<Segnalazione, GeoPoint>> {
    val lista = mutableListOf<Pair<Segnalazione, GeoPoint>>()
    val formatter = DataFormatter()
    try {
        val inputStream: InputStream = context.assets.open("Pipistrelli 2025.xlsx")
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)
        
        var headerIdx = -1
        for (i in 0..50) {
            val r = sheet.getRow(i) ?: continue
            for (j in 0..20) {
                if (formatter.formatCellValue(r.getCell(j)).lowercase().contains("specie")) {
                    headerIdx = i; break
                }
            }
            if (headerIdx != -1) break
        }
        
        if (headerIdx == -1) return emptyList()

        val headerRow = sheet.getRow(headerIdx)
        val colMap = mutableMapOf<String, Int>()
        val headerNames = mutableListOf<String>()
        for (j in 0 until headerRow.lastCellNum.toInt()) {
            val cell = headerRow.getCell(j)
            val name = formatter.formatCellValue(cell).lowercase().trim()
            headerNames.add(name)
        }

        fun findIndex(vararg targets: String): Int {
            for (t in targets) {
                val idx = headerNames.indexOf(t)
                if (idx != -1) return idx
            }
            for (t in targets) {
                val idx = headerNames.indexOfFirst { it.contains(t) }
                if (idx != -1) return idx
            }
            return -1
        }

        colMap["specie"] = findIndex("specie animale", "specie")
        colMap["data"] = findIndex("data segnalazione", "data")
        colMap["ora"] = findIndex("ora")
        colMap["loc"] = findIndex("località", "localit")
        colMap["comune"] = findIndex("comune")
        colMap["note"] = findIndex("condizioni al recupero", "note")
        colMap["prov"] = findIndex("provincia")
        colMap["stato"] = findIndex("stato")
        colMap["lat"] = findIndex("latitude", "lat")
        colMap["lon"] = findIndex("longitude", "lon", "lng")

        for (i in (headerIdx + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val specie = formatter.formatCellValue(row.getCell(colMap["specie"] ?: -1)).trim()
            val dRaw = formatter.formatCellValue(row.getCell(colMap["data"] ?: -1)).trim()
            
            if (specie.isBlank() && dRaw.isBlank()) continue

            val dFormatted = dRaw.replace("/", ".")
            val oRaw = formatter.formatCellValue(row.getCell(colMap["ora"] ?: -1))
            val dataFull = if (oRaw.isNotBlank()) "Segnalazione del $dFormatted alle ore $oRaw" else "Segnalazione del $dFormatted"
            
            val localita = formatter.formatCellValue(row.getCell(colMap["loc"] ?: -1)).ifBlank { "-" }
            val comuneRaw = formatter.formatCellValue(row.getCell(colMap["comune"] ?: -1)).trim()
            var provincia = formatter.formatCellValue(row.getCell(colMap["prov"] ?: -1)).trim()

            // Recupero provincia (ora che l'Excel è compilato fisicamente)
            if (provincia.isBlank() || provincia.lowercase() == "nan" || provincia == "-" || provincia.length > 2) {
                provincia = ComuniDatabase.cercaProvincia(comuneRaw, localita)
            }
            val comune = if (comuneRaw.isBlank()) "-" else comuneRaw
            val stato = formatter.formatCellValue(row.getCell(colMap["stato"] ?: -1)).ifBlank { "-" }
            val note = formatter.formatCellValue(row.getCell(colMap["note"] ?: -1)).ifBlank { "-" }
            
            var lat = getCellDouble(row, colMap["lat"] ?: -1)
            var lon = getCellDouble(row, colMap["lon"] ?: -1)

            if (lat == 0.0) {
                lat = 45.5479 + (Math.random() - 0.5) * 0.1
                lon = 11.5446 + (Math.random() - 0.5) * 0.1
            }
            
            lista.add(Segnalazione(dataFull, specie.ifBlank { "Pipistrello" }, localita, comune, provincia, stato, note, lat, lon) to GeoPoint(lat, lon))
        }
        workbook.close()
    } catch (e: Exception) { Log.e("BatMaps", "Errore: ${e.message}") }
    return lista
}

fun getCellDouble(row: Row, index: Int): Double {
    if (index < 0) return 0.0
    val cell = row.getCell(index) ?: return 0.0
    return try {
        when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
            CellType.FORMULA -> row.sheet.workbook.creationHelper.createFormulaEvaluator().evaluateInCell(cell).numericCellValue
            else -> 0.0
        }
    } catch (e: Exception) { 0.0 }
}
