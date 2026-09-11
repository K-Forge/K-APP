import SwiftUI

/// La escala tipográfica del diseño.
///
/// Es la misma en iOS y en Android; lo único que cambia es la familia (aquí SF Pro, allá
/// Roboto). Los tamaños salen de `docs/design/mobile/Tokens.dc.html`.
///
/// Se usan tamaños fijos y no `Font.title`/`.body` porque la maqueta está cuadrada al punto
/// y las dos plataformas tienen que dar el mismo resultado. Eso sí: nada de esto escala con
/// Dynamic Type todavía — está anotado como pendiente en el README.
enum KType {
    /// 26 / 700 — "Hola, Pepe".
    static let greeting = Font.system(size: 26, weight: .bold)
    /// 20 / 600 — el nombre de una materia.
    static let subject = Font.system(size: 20, weight: .semibold)
    /// 17 / 600 — encabezado de sección.
    static let section = Font.system(size: 17, weight: .semibold)
    /// 15 / 600 — fila de lista, etiqueta de campo, botón.
    static let rowTitle = Font.system(size: 15, weight: .semibold)
    /// 15 / 400 — cuerpo.
    static let body = Font.system(size: 15)
    /// 14 / 400 — enlaces y datos secundarios.
    static let link = Font.system(size: 14, weight: .medium)
    /// 13 / 400 — metadatos.
    static let meta = Font.system(size: 13)
    /// 12 / 400 — etiquetas.
    static let label = Font.system(size: 12)
    /// 11 / 600 con tracking — el único texto en versalitas de la app.
    static let eyebrow = Font.system(size: 11, weight: .semibold)
    /// 10 / 600 — texto de la barra inferior.
    static let tab = Font.system(size: 10, weight: .semibold)
}

/// Radios, alturas y espaciados. También compartidos con Android.
enum KMetrics {
    static let cardRadius: CGFloat = 18
    static let rowRadius: CGFloat = 14
    static let buttonRadius: CGFloat = 14
    static let fieldRadius: CGFloat = 12
    static let tileRadius: CGFloat = 16

    static let fieldHeight: CGFloat = 52
    /// 44 es el mínimo táctil de iOS. Ningún control baja de aquí.
    static let buttonHeight: CGFloat = 44
    static let rowHeight: CGFloat = 58

    static let screenPadding: CGFloat = 16
    static let cardPadding: CGFloat = 16

    /// La barra flotante: alto, margen lateral, separación del borde inferior y radio.
    static let tabBarHeight: CGFloat = 62
    static let tabBarInset: CGFloat = 16
    static let tabBarBottom: CGFloat = 26
    static let tabBarRadius: CGFloat = 22
}

extension View {
    /// La tarjeta blanca del diseño: fondo, radio y la sombra morada tenue.
    func kCard(radius: CGFloat = KMetrics.cardRadius) -> some View {
        background(KColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .shadow(color: KColor.purple.opacity(0.07), radius: 5, x: 0, y: 2)
    }
}

/// El filo tricolor de la K: rosa, verde y azul en partes iguales.
///
/// Va donde la hoja blanca se monta sobre el morado. Es el recurso de marca de la app;
/// sustituye al degradado que tenía el login viejo en web.
struct KTricolorEdge: View {
    var height: CGFloat = 4

    var body: some View {
        HStack(spacing: 0) {
            KColor.pink
            KColor.green
            KColor.blue
        }
        .frame(height: height)
    }
}
