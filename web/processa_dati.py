import pandas as pd
import os

def processa_excel(path):
    if not os.path.exists(path): return
    print(f"--- PULIZIA COLONNE EXCEL: {path} ---")
    try:
        df = pd.read_excel(path)
        # Pulizia nomi colonne (toglie spazi e invii)
        df.columns = [str(c).replace('\n', ' ').strip() for c in df.columns]
        # Salva senza modificare le coordinate
        df.to_excel(path, index=False)
        print("✅ Colonne pulite con successo.")
    except Exception as e:
        print(f"❌ Errore bonifica: {e}")

if __name__ == "__main__":
    processa_excel('web/Pipistrelli 2025.xlsx')
