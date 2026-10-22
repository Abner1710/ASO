# ⚡ Simulador del Subsistema de Entrada/Salida (I/O Kernel Subsystem)

![Java](https://img.shields.io/badge/Java-17%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-FF8000?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Apache%20Maven-3.9%2B-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-0078D6?style=for-the-badge&logo=windows&logoColor=white)
![LaTeX](https://img.shields.io/badge/Overleaf-pdfLaTeX-008080?style=for-the-badge&logo=overleaf&logoColor=white)

> **Universidad Nacional de Ingeniería (UNI)**  
> **Área de Conocimiento de Tecnología de la Información (DACTIC)**  
> **Asignatura:** Arquitectura y Sistemas Operativos (ASO)  
> **Docente:** Ing. Jacqueline Montes  
> **Grupo No. 14:**
> * **Abner Amarus Fonseca Sequeira** (Carné: `2023-0608U`)
> * **María José Guzmán Castillo** (Carné: `2023-0603U`)

---

## 📌 Descripción del Proyecto

Este proyecto consiste en un **Simulador del Subsistema de Entrada/Salida de un Sistema Operativo** desarrollado en **Java 17 LTS** y **JavaFX**, diseñado con una arquitectura modular desacoplada (Patrón MVC) y una interfaz de usuario moderna de alto impacto visual (*Dark Cyberpunk & SaaS aesthetic*).

El simulador reproduce de manera determinista y didáctica el desacoplamiento temporal existente entre el procesador central y los periféricos de hardware, emulando la gestión de colas de peticiones FIFO, la transferencia incremental por búfer de datos, y el ciclo completo de señales de interrupción de hardware (**IRQ**) y rutinas de atención (**ISR**).

---

## ✨ Características Principales

* 🖥️ **Interfaz Gráfica Reactiva y Futurista:** Estilo Cyberpunk Dark con paleta Slate/Neón, bordes con efecto *glow*, micro-animaciones en transiciones y renderizado acelerado por hardware (*Prism*).
* 🎨 **Iconos Vectoriales SVG Nativos:** Integración de trazados SVG de alta fidelidad sin dependencias externas pesadas (`IconHelper.java`), escalables y nítidos en monitores Full HD y 4K.
* 📊 **Panel Superior de Telemetría (KPIs en Tiempo Real):** Monitoreo en vivo de:
  * Peticiones totales emitidas.
  * Procesos en espera en la cola FIFO.
  * Carga activa de controladores de hardware.
  * Estado y frecuencia de latido del kernel (`1500 ms`).
* 📋 **Gestor de Colas Gráfico (*Wait Queue*):** Fábrica de celdas personalizadas (`ListCell`) con badges coloreados por tipo de operación (`READ` en cian / `WRITE` en púrpura), destino de hardware y carga útil en bytes.
* 💾 **Simulación de Controladores de Hardware Dedicados:** Tarjetas interactivas con estados discretos (`LIBRE`, `OCUPADO`, `ISR PENDIENTE`), barras de progreso redondeadas con porcentaje y micro-animación de pulso (*Scale Transition*) ante eventos físicos.
* ⚡ **Generador de Carga / Stress Test (Turbo):** Timeline automatizado para pruebas de estrés bajo condiciones de saturación de cola y análisis de cuellos de botella.
* 📟 **Terminal de Telemetría DevTools:** Consola de eventos del kernel con sellos de tiempo precisos (`HH:mm:ss`), botón de reseteo rápido y trazabilidad de interrupciones.
* 📄 **Informe Técnico Académico Completo en LaTeX:** Archivo maestro `documento_proyecto_final.tex` listo para Overleaf con más de 25 páginas, 5 diagramas arquitectónicos modelados en TikZ, análisis cuantitativo bajo la Ley de Little y modelo $M/M/1$.

---

## 🗂️ Estructura del Repositorio

```text
.
├── pom.xml                                   # Descriptor de dependencias y plugins Maven
├── README.md                                 # Documentación técnica del repositorio
├── .gitignore                                # Filtros de exclusión para Maven, IDEs y LaTeX
├── UNI_Imagen.png                            # Isotipo institucional de la Universidad
├── documento_proyecto_final.tex              # Informe técnico de curso en LaTeX (Overleaf)
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java              # Configuración del sistema modular Java (JPMS)
│       │   └── ni/edu/uni/aso/
│       │       ├── App.java                  # Punto de entrada de la aplicación JavaFX
│       │       ├── core/                     # Capa del Dominio del Kernel (Lógica pura de SO)
│       │       │   ├── DeviceStatus.java     # Estados del periférico (LIBRE, OCUPADO, INTERRUMPIDO)
│       │       │   ├── IODevice.java         # Modelo de controlador y búfer de dispositivo
│       │       │   ├── IORequest.java        # Modelo de paquete de petición de E/S
│       │       │   ├── IOSubsystem.java      # Planificador central, colas y motor de reloj
│       │       │   └── OperationType.java    # Enumeración de operaciones (LECTURA, ESCRITURA)
│       │       └── ui/                       # Capa de Presentación (Controlador y Vistas)
│       │           ├── DashboardController.java # Coordinador de la interfaz, eventos y animaciones
│       │           └── IconHelper.java       # Proveedor central de vectores SVG nativos
│       └── resources/
│           └── ni/edu/uni/aso/
│               ├── Dashboard.fxml            # Estructura visual de componentes en FXML
│               └── styles.css                # Hoja de estilos CSS Dark Cyberpunk
```

---

## ⚙️ Mapeo Conceptual de Primitivas de Sistemas Operativos

| Primitiva / Concepto POSIX | Función en el Kernel Real | Implementación en el Simulador Java |
| :--- | :--- | :--- |
| `read(fd, buf, count)` | Solicita lectura de bloques hacia el espacio de usuario. | `handleGenerateRequest(OperationType.LECTURA)` |
| `write(fd, buf, count)` | Escribe un flujo de datos hacia un dispositivo. | `handleGenerateRequest(OperationType.ESCRITURA)` |
| `sleep() / wait_event()` | Suspende el hilo colocándolo en estado *Bloqueado*. | Encolamiento de la petición en `requestQueue` hasta liberación. |
| **Línea de Interrupción (IRQ)** | Señal eléctrica emitida por el controlador al bus de la CPU. | Transición a `DeviceStatus.INTERRUMPIDO` al alcanzar el *payload*. |
| **Rutina ISR (*Top/Bottom Half*)** | Código del kernel que atiende la IRQ y limpia el búfer. | Ejecución de `device.resetInterrupt()` en el siguiente ciclo `tick()`. |
| **Registros de Control / Buffering** | Gestión de capacidad física temporal de transferencia. | Atributos `currentBuffer` y `bufferSize` en `IODevice`. |

---

## 🚀 Requisitos y Guía de Ejecución

### Prerrequisitos
* **Java Development Kit (JDK):** Versión 17 LTS o superior.
* **Apache Maven:** Versión 3.8+ instalada y configurada en el `PATH`.

### Instrucciones de Ejecución

1. **Clonar el repositorio:**
   ```powershell
   git clone https://github.com/Abner1710/ASO.git
   cd ASO
   ```

2. **Compilar el proyecto:**
   ```powershell
   mvn clean compile
   ```

3. **Ejecutar el simulador interactivo:**
   ```powershell
   mvn javafx:run
   ```

---

## 📑 Compilación del Informe en LaTeX (Overleaf)

El informe técnico formal del proyecto se encuentra en el archivo [`documento_proyecto_final.tex`](./documento_proyecto_final.tex).

* Diseñado con tipografía **Arial de 11pt**, interlineado **1.5**, márgenes institucionales (3.0 cm izquierdo para encuadernación, 2.5 cm en los demás bordes).
* Compatible con **pdfLaTeX** en Overleaf.
* Contiene **5 diagramas TikZ** completos:
  1. *Diagrama de Transición de Estados del Proceso (Bloqueo por E/S)*.
  2. *Arquitectura en 5 Capas del Software de Entrada/Salida*.
  3. *Máquina de Estados Finitos del Controlador de Dispositivo*.
  4. *Diagrama de Secuencia Temporal (Usuario - Kernel - Hardware - Telemetría)*.
  5. *Árbol de Decisión y Flujo de Despacho del Planificador `tick()`*.

---

## 👥 Autores y Créditos

* **Abner Amarus Fonseca Sequeira** — [GitHub: @Abner1710](https://github.com/Abner1710)
* **María José Guzmán Castillo** — [GitHub: @MariaGuzmanC](https://github.com/MariaGuzmanC)

*Facultad de Electrotecnia y Computación / Área de Conocimiento de Tecnología de la Información (DACTIC)*  
**Universidad Nacional de Ingeniería (UNI) — Managua, Nicaragua, 2026.**
