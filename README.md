## This is an open-source android protoype for NSFW detection, integrated with advanced deep learning models.

### A. For non-development usage please refer to the following bundled release posted in the repository

### B. For development usage and contribution, refer to the guide accordingly:

#### Requirements

- Android studio (2024.3.1 or later versions)
- Java SDK 34
- Gradle dependency list (this would be pre-compiled once the repository is emulated locally)
- Android 14+ devices (physical and/or emulated) (API 34+)
- Used models for image: Yolov5, Yolov10, Yolov 11, Yolov12
- Used models for text: LSTM, BILSTM, BERT (& variants)

##### NOTE: In compliances to the new [google-policy 2025](https://support.google.com/googleplay/android-developer/answer/16296680?hl=en), the project targets Android 15 as the minimum android version. This also allows us to use up-to-date native APIs and gain access to higher level permissions such as Foreground services and System Window.

### The project is emulated and tested on the following devices:

    - Google Pixel 9 (Android 15)
    - Samsung A34 (Android 15)

### Average benchmark per model:

- YOLOv5 (320x320) - 21fps
- YOLOv11 & 12 (320x320) - 18fps
- YOLOv10 - 5ps

### How to test the app in android studio:

1. Install the Java SDK 34 in android studio.
2. Open android studio and clone the repository (git clone https://github.com/xinzhao2627/THESIS-R).
    - use `finalthesisapp` for testing.
3. Relocate the downloaded models to the assets folder.
4. Sync the gradle into the project.
5. Use supported or suggested devices (with GPU) to test the application.
6. For contributions, kindly fork the repo and open a pull request.
