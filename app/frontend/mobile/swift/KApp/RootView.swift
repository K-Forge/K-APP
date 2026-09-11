import SwiftUI

/// Los cinco destinos, en el orden en que aparecen de izquierda a derecha.
/// Inicio va al centro a propósito.
enum KTab: Int, CaseIterable, Identifiable {
    case perfil, semaforo, inicio, mapa, horario

    var id: Int { rawValue }

    var title: String {
        switch self {
        case .perfil: "Perfil"
        case .semaforo: "Semáforo"
        case .inicio: "Inicio"
        case .mapa: "Mapa"
        case .horario: "Horario"
        }
    }

    var symbol: String {
        switch self {
        case .perfil: "person"
        case .semaforo: "square.grid.2x2"
        case .inicio: "house"
        case .mapa: "mappin.and.ellipse"
        case .horario: "calendar"
        }
    }
}

struct RootView: View {
    /// Provisional. Cuando exista la capa de red esto pasa a ser el token guardado en el
    /// llavero: la sesión del estudiante se queda en el teléfono y no expira sola.
    /// Ver el pendiente de refresh token en el README.
    @State private var isSignedIn = false

    var body: some View {
        if isSignedIn {
            MainShell()
        } else {
            LoginView { isSignedIn = true }
        }
    }
}

/// La app con sesión: la pantalla activa y la barra flotante encima.
///
/// No se usa `TabView` porque la barra del diseño flota, va separada de los bordes y lleva
/// su propia sombra. `TabView` ancla la suya al borde inferior y no deja darle esa forma.
struct MainShell: View {
    @State private var tab: KTab = .inicio

    var body: some View {
        ZStack(alignment: .bottom) {
            KColor.background.ignoresSafeArea()

            Group {
                switch tab {
                case .inicio: HomeView()
                case .perfil: PlaceholderView(tab: .perfil)
                case .semaforo: PlaceholderView(tab: .semaforo)
                case .mapa: PlaceholderView(tab: .mapa)
                case .horario: PlaceholderView(tab: .horario)
                }
            }

            KTabBar(selection: $tab)
                .padding(.horizontal, KMetrics.tabBarInset)
                .padding(.bottom, KMetrics.tabBarBottom)
        }
        .ignoresSafeArea(.keyboard)
    }
}

struct KTabBar: View {
    @Binding var selection: KTab

    var body: some View {
        HStack(spacing: 0) {
            ForEach(KTab.allCases) { tab in
                Button {
                    selection = tab
                } label: {
                    VStack(spacing: 3) {
                        Image(systemName: tab.symbol)
                            .font(.system(size: 20, weight: selection == tab ? .semibold : .regular))
                        Text(tab.title)
                            .font(KType.tab)
                    }
                    .foregroundStyle(selection == tab ? KColor.action : KColor.iconIdle)
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(height: KMetrics.tabBarHeight)
        .background(KColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KMetrics.tabBarRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: KMetrics.tabBarRadius, style: .continuous)
                .stroke(KColor.border.opacity(0.6), lineWidth: 1)
        )
        .shadow(color: KColor.purple.opacity(0.20), radius: 13, x: 0, y: 8)
    }
}

/// Lo que todavía no existe. Semáforo, Mapa, Horario y Perfil se diseñan después de
/// Login e Inicio; los wireframes viejos están en `docs/design/`.
struct PlaceholderView: View {
    let tab: KTab

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: tab.symbol)
                .font(.system(size: 32, weight: .light))
                .foregroundStyle(KColor.textFaint)
            Text(tab.title)
                .font(KType.section)
                .foregroundStyle(KColor.text)
            Text("Sin diseñar todavía.")
                .font(KType.meta)
                .foregroundStyle(KColor.textSoft)
        }
    }
}

#Preview {
    MainShell()
}
