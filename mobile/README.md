# Android Studio project docs

## This is an open-source android protoype for NSFW detection, integrated with advanced deep learning models.

### A. For non-development usage please refer to the following bundled release posted in the repository

### B. For development usage and contribution, refer to the guide accordingly:

#### Requirements

    - Android studio (2024.3.1 or later versions)
    - Java SDK 21
    - Gradle dependency list (this would be pre-compiled once the repository is emulated locally)
    - Android 15+ devices (physical and/or emulated) (API 35+)
    - We suggest to enable "developer options > hardware acceleration > blur" for the censor clarity.
    - The [pretrained models](https://drive.google.com/drive/folders/1vGhCTlcZLa0jQHkNevIFI-L5dAbCEUh0?usp=sharing) or custom models that you must move to the assets folder (must have available metadata which you can generate following tflite-support documentation)

##### NOTE: In compliances to the new [google-policy 2025](https://support.google.com/googleplay/android-developer/answer/16296680?hl=en), the project targets Android 15 as the minimum android version. This also allows us to use up-to-date native APIs and gain access to higher level permissions such as Foreground services and System Window.

### The project is emulated and tested on the following devices:

    - Google Pixel 9 (Android 15)
    - Samsung A34 (Android 15)

### Average benchmark per model:

### How to test the app in android studio:

    1. Install the Java SDK 21 in android studio.
    2. Open android studio and clone the repository (git clone https://github.com/baldeoJV/THESIS-.git).
    3. Relocate the downloaded models to the assets folder.
    4. Sync the gradle into the project.
    5. Use supported or suggested devices to test the application.
    6. For contributions, kindly fork the repo and open a pull request.
