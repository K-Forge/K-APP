# KApp · iOS

Cliente nativo en Swift + SwiftUI. Se construye contra los contratos de
[`docs/api/`](../../../../docs/api/) y contra las maquetas de
[`docs/design/mobile/`](../../../../docs/design/mobile/).

## Abrir

```bash
open app/frontend/mobile/swift/KApp.xcodeproj
```

Requiere Xcode 16 o superior (el proyecto usa grupos sincronizados con el sistema de
archivos). Target mínimo **iOS 17**, solo iPhone, solo vertical.

> **Antes de compilar por primera vez**, si nunca has aceptado las licencias de Xcode en
> esta máquina:
>
> ```bash
> sudo xcodebuild -license accept
> ```

## Cómo está organizado

```
KApp/
├── KAppApp.swift          @main
├── RootView.swift         sesión sí/no, y la barra flotante de cinco destinos
├── DesignSystem/
│   ├── KColor.swift       la paleta y el papel de cada color
│   └── KType.swift        escala tipográfica, radios, alturas, la tarjeta y el filo tricolor
└── Features/
    ├── Login/LoginView.swift
    └── Home/
        ├── HomeModels.swift   espejo de ClassOccurrence y ProgressSummary
        └── HomeView.swift
```

**No hay que tocar el `.pbxproj` para agregar archivos.** El target usa un grupo
sincronizado: todo lo que aparezca dentro de `KApp/` entra solo al compilar. Eso también
significa que dos personas pueden agregar archivos en paralelo sin pelearse por el
proyecto en cada merge.

## Reglas que no son negociables

Salen de la maqueta, no de una preferencia:

- **El rosa `#D51A65` significa "tocable"**. Botón primario, pestaña activa, enlaces. Nada
  más se pinta de rosa — por eso ninguna materia lo usa.
- **El color de una materia lo manda el servidor**, en `ClassOccurrence.color`. El cliente
  lo pinta; no lo elige ni lo calcula. Para eso está `Color(apiHex:)`.
- **Nunca texto blanco sobre el verde `#C9D329`**: da 1.5:1 y desaparece al sol. Sobre verde
  va el morado de marca.
- **Ningún control por debajo de 44 pt** de alto.
- La app no dibuja status bar ni indicador de inicio: ese espacio lo pinta el sistema.

## Lo que ya está y lo que no

Hecho: sistema de diseño completo, la estructura de navegación, el login (con sus cuatro
estados) y el inicio con datos de ejemplo.

Falta:

1. **La capa de red.** No hay ni un `URLSession` todavía. `LoginView.submit()` y los datos
   de `HomeView` están marcados con lo que tienen que llamar.
2. **La sesión.** Hoy es un `@State` booleano. Va al llavero, y no expira: la del estudiante
   se queda guardada en el teléfono.
3. **Dynamic Type.** La escala usa tamaños fijos para cuadrar con la maqueta y con Android.
   Habrá que decidir cómo se comporta cuando el usuario sube el tamaño de letra.
4. Semáforo, Mapa, Horario y Perfil: sin diseñar.

## Pendiente que no depende de nosotros

`auth.openapi.yaml` dice que **no hay refresh token** y que al vencer `expiresIn` (3600 s)
el cliente debe volver a pedir credenciales. La sesión que no expira necesita que
auth-service ofrezca refresh o un token largo para `ROLE_STUDENT`, dejando la expiración
corta para `ROLE_ADMIN`.
