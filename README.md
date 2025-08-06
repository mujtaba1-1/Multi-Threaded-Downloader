<h1 align="center" id="title">Multi-Threaded File Downloader</h1>

<p id="description">A desktop application built with Java Swing that allows users to download files efficiently from a given URL using multiple threads.</p>

  
  
<h2>🧐 Features</h2>

Here're some of the project's best features:

*   ✅ Multi-threaded downloading
*   ⏸️ Pause and resume downloads
*   ❌ Cancel and clean up in-progress downloads
*   🧠 Smart file name detection (from HTTP headers or URL)
*   📊 Real-time progress tracking with a progress bar
*   💾 Handles file naming conflicts automatically

<h2>📸 GUI Preview</h2>

<img width="530" height="288" alt="image" src="https://github.com/user-attachments/assets/e30ef259-c2e1-4110-a4b4-316894e33c2e" />


<h2>🛠️ Installation Steps:</h2>

<p>1. Clone the repo:</p>

```
git clone https://github.com/mujtaba1-1/Multi-Threaded-Downloader.git
```

<p>2. Compile &amp; Run</p>

```
javac *.java
java Main
```
Or, run the `Main` class from your preferred Java IDE

<h2>📂 Download Destination</h2>

All files are saved to a local folder name `downloads/`. If a file with the same name exists,
the app appends `(1)`, `(2)`, etc., to avoid overwriting

<h2>💻 Built with</h2>

Technologies used in the project:

*   Java 8+
*   Java Swing
*   Multithreading and ExecutorService
*   HTTP(S) connections via HttpURLConnection and HttpsURLConnection

