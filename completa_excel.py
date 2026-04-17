import pandas as pd
import json
import os
import shutil

def pulisci_excel():
    excel_path = 'Pipistrelli 2025.xlsx'
    json_path = 'app/src/main/assets/comuni_italiani.json'

    if not os.path.exists(excel_path):
        print(f"Errore: {excel_path} non trovato.")
        return

    # Backup di sicurezza
    shutil.copy(excel_path, 'Pipistrelli 2025_BACKUP.xlsx')
    print("✅ Backup creato: Pipistrelli 2025_BACKUP.xlsx")

    # Carica database comuni per il confronto
    with open(json_path, encoding='utf-8') as f:
        db_comuni = json.load(f)

    # Carica Excel (cercando la riga di intestazione corretta)
    df = pd.read_excel(excel_path)

    # Trova l'indice della riga che contiene le intestazioni vere
    header_row_idx = 0
    for i, row in df.iterrows():
        row_str = str(row.values).lower()
        if 'specie' in row_str or 'comune' in row_str:
            header_row_idx = i
            break

    # Ricarica l'excel con l'header corretto
    df = pd.read_excel(excel_path, header=header_row_idx)

    def correggi_riga(row):
        comune_attuale = str(row.get('Comune', '')).strip().lower()
        localita = str(row.get('Località', '')).strip().lower()
        provincia = str(row.get('Provincia', '')).strip().upper()

        # Se il comune è vuoto o un trattino, cerchiamo nella località
        if comune_attuale in ['', 'nan', '-', 'None']:
            for nome_comune, info in db_comuni.items():
                if len(nome_comune) > 3 and nome_comune in localita:
                    print(f"   -> Trovato '{nome_comune}' in località: '{localita}'")
                    row['Comune'] = nome_comune.capitalize()
                    if provincia in ['', 'NAN', '-', 'NONE']:
                        row['Provincia'] = info['prov']
                    break

        # Correzione province mancanti se il comune c'è
        elif comune_attuale in db_comuni and (provincia in ['', 'NAN', '-', 'NONE']):
            row['Provincia'] = db_comuni[comune_attuale]['prov']

        return row

    print("Analisi e correzione in corso...")
    df = df.apply(correggi_riga, axis=1)

    # Salva in tutte le cartelle
    output_paths = [
        'Pipistrelli 2025.xlsx',
        'web/Pipistrelli 2025.xlsx',
        'app/src/main/assets/Pipistrelli 2025.xlsx'
    ]

    for out in output_paths:
        if os.path.exists(os.path.dirname(out) or '.'):
            df.to_excel(out, index=False)
            print(f"✅ Salvato: {out}")

if __name__ == "__main__":
    pulisci_excel()
