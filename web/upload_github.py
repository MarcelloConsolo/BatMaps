import os
import subprocess

def find_git():
    # Percorsi comuni di Git su Windows
    paths = [
        r"C:\Program Files\Git\bin\git.exe",
        r"C:\Program Files (x86)\Git\bin\git.exe",
        os.path.expanduser(r"~\AppData\Local\Programs\Git\bin\git.exe")
    ]
    for path in paths:
        if os.path.exists(path):
            return path
    return "git" # Fallback

def upload_to_github():
    git_path = find_git()
    print(f"--- USANDO GIT DA: {git_path} ---")

    try:
        # Aggiunge tutto
        subprocess.run([git_path, "add", "."], check=True)
        # Commit
        subprocess.run([git_path, "commit", "-m", "Riparazione province e Bassano"], check=True)
        # Push
        subprocess.run([git_path, "push"], check=True)

        print("✅ UPLOAD COMPLETATO CON SUCCESSO!")

    except Exception as e:
        print(f"❌ ERRORE: {e}")

if __name__ == "__main__":
    upload_to_github()
