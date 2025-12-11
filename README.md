# 🎓 TuCarnet UFPS

Aplicación móvil de carnet digital para estudiantes de la Universidad Francisco de Paula Santander (UFPS).

[![📥 Descargar
APK](https://img.shields.io/badge/Descargar-APK-blue?style=for-the-badge)](https://github.com/BryanVeraDev/TuCarnetApp/releases/download/v0.1.0/presaber.apk)

> Versión: **v0.1.0 (Release)**\
> Esta APK es una versión de prueba generada desde Android Studio. No es
> apta para producción.

------------------------------------------------------------------------

## 📱 Características

- 🔐 Autenticación segura con Firebase
- 🎫 Carnet digital con código QR dinámico (renovación cada hora)
- 📸 Validación de carnets mediante escaneo QR
- ✅ Verificación biométrica obligatoria
- 🔄 Sincronización automática con sistemas institucionales

## 🚀 Instalación y Ejecución

### 1️⃣ Clonar el repositorio
```bash
git clone https://github.com/BryanVeraDev/TuCarnetApp.git
cd TuCarnetApp
```

### 2️⃣ Abrir en Android Studio

1. Abrir Android Studio
2. File → Open → TuCarnetApp
3. Esperar sincronización de Gradle

### 3️⃣ Configurar variables locales

Crea o edita `local.properties` en la raíz:
```properties
sdk.dir=<ruta_del_sdk>
```

Este archivo está en `.gitignore`.

### 4️⃣ Compilar el proyecto
```bash
./gradlew assembleDebug
```

### 5️⃣ Ejecutar la app

Conecta un dispositivo Android o usa un emulador, luego presiona **Run ▶** en Android Studio.

## 🛠️ Tecnologías

- **Lenguaje**: Kotlin
- **UI**: XML + Material Design
- **Autenticación**: Firebase Authentication
- **Backend**: API REST personalizada
- **QR**: ZXing Android Embedded
- **Build**: Gradle 8.13

## 📱 Requisitos

- Android 7.0+ (API 24 o superior)
- Conexión a internet
- Cámara (para validación de carnets)

## 🤝 Contribuidores

**Bryan Vera**  
GitHub: [@BryanVeraDev](https://github.com/BryanVeraDev)

## 📝 Notas

- Esta es una **versión Beta (0.1.0)** para pruebas internas
- Se irán agregando nuevas funcionalidades en próximas versiones
- Reporta bugs en [Issues](https://github.com/BryanVeraDev/TuCarnetApp/issues)

## 📄 Licencia

Proyecto de uso/desarrollo académico exclusivo para la comunidad UFPS.  
```
---