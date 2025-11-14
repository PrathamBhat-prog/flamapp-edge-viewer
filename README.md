# Edge Viewer App (Android + OpenCV + JNI)

A lightweight Android application that captures an image from the camera and generates an **edge-detected version** using **OpenCV (C++)**, **JNI**, and the **Camera2 API**.

This project demonstrates a complete end-to-end flow:
Camera frame → Bitmap → ByteArray → JNI → OpenCV native processing → Processed output rendering.

---

## 📌 Features

### ✔ Live Camera Preview  
Full-screen real-time camera feed using `TextureView`.

### ✔ Capture Image  
Captures the current frame and converts it into a Bitmap.

### ✔ Native Edge Detection (C++ + OpenCV)  
The captured Bitmap is processed inside a native C++ layer using:
- OpenCV image processing  
- Sobel / Canny edge detection  
- RGBA → Grayscale → Edges → RGBA pipeline

### ✔ Processed Image Display  
Edge-detected output is shown in an ImageView.

### ✔ Retake Function  
Allows returning to live camera mode and capturing again.

---

## 🛠 Tech Stack

### **Android**
- Kotlin  
- Camera2 API  
- TextureView  
- ConstraintLayout  

### **Native Layer**
- C++  
- OpenCV Android SDK  
- JNI (Java Native Interface)  
- CMake  

### **Build Tools**
- Gradle  
- Android Studio  

---
