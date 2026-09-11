import SwiftUI

/// Los colores de la K y el papel que cumple cada uno.
///
/// Los siete hex salen de `docs/K-COLORS.md`. No se inventan colores aquí: si hace falta
/// un tono nuevo, se deriva de estos con opacidad, y si eso no alcanza, se discute y se
/// agrega primero al documento y a `docs/design/mobile/Tokens.dc.html`.
enum KColor {

    // MARK: - La paleta

    /// Rosa. En la app significa *tocable* y nada más.
    static let pink = Color(hex: 0xD51A65)
    /// Verde. Solo como relleno: el texto encima va en `brand`, nunca blanco.
    static let green = Color(hex: 0xC9D329)
    /// Azul.
    static let blue = Color(hex: 0x539392)
    /// Rosa + azul. El morado de marca.
    static let purple = Color(hex: 0x522567)
    /// Rosa + verde.
    static let red = Color(hex: 0xB62325)
    /// Verde + azul.
    static let forest = Color(hex: 0x3E823E)
    /// Los tres superpuestos.
    static let brown = Color(hex: 0x592E2A)

    // MARK: - Papeles

    /// Marca: la banda superior del inicio y el campo del login.
    static let brand = purple
    /// Acción: botón primario, pestaña activa, enlaces. Solo esto.
    static let action = pink
    static let onAction = Color.white

    /// Estados del semáforo.
    static let passed = forest
    static let inProgress = green
    static let pending = Color(hex: 0xE7E2EA)

    static let error = red

    /// Superficies. El fondo tiene un tinte morado: no es gris neutro.
    static let background = Color(hex: 0xF7F4F8)
    static let surface = Color.white

    static let text = Color(hex: 0x1C1420)
    static let textSoft = Color(hex: 0x6B6472)
    static let textFaint = Color(hex: 0xA29BA9)
    static let border = Color(hex: 0xE7E2EA)

    /// Gris de los íconos inactivos de la barra.
    static let iconIdle = Color(hex: 0x8E8794)
}

extension Color {
    /// `Color(hex: 0xD51A65)` — para poder pegar los hex del documento tal cual.
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }

    /// El color que manda el servidor en `ClassOccurrence.color`, por ejemplo `"#539392"`.
    ///
    /// El cliente **no** elige el color de una materia: lo pinta. Si la cadena no se puede
    /// leer, cae en el azul en vez de reventar, porque una materia sin color en la lista es
    /// mejor que una pantalla en blanco.
    init(apiHex: String?) {
        let cleaned = (apiHex ?? "").trimmingCharacters(in: .whitespaces).replacingOccurrences(of: "#", with: "")
        guard cleaned.count == 6, let value = UInt32(cleaned, radix: 16) else {
            self = KColor.blue
            return
        }
        self.init(hex: value)
    }
}
