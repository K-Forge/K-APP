import SwiftUI

struct HomeView: View {
    /// Provisional: sale de `SampleData` hasta que exista el cliente de red.
    /// Después, `GET /api/schedule/me/day` y `GET /api/semaphore/me/summary`, que son
    /// servicios distintos y fallan por separado — cada bloque necesita su propio estado
    /// de carga y de error.
    private let classes = SampleData.today
    private let progress = SampleData.progress
    private let firstName = "Pepe"
    private let initials = "PP"

    /// La primera del día es la tarjeta grande; el resto va en la lista de abajo.
    private var next: ClassOccurrence? { classes.first }
    private var rest: [ClassOccurrence] { Array(classes.dropFirst()) }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                band
                content
            }
        }
        .background(KColor.background)
        .ignoresSafeArea(edges: .top)
    }

    // MARK: - Banda de marca

    private var band: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(KColor.surface)
                .frame(width: 40, height: 40)
                .overlay(
                    Image("KonradLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 30, height: 30)
                )

            Text("Hola, \(firstName)")
                .font(KType.greeting)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)

            Circle()
                .fill(.white.opacity(0.18))
                .frame(width: 34, height: 34)
                .overlay(Circle().stroke(.white.opacity(0.28), lineWidth: 1))
                .overlay(
                    Text(initials)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.white)
                )
        }
        .padding(.horizontal, 20)
        .padding(.top, 54)
        .padding(.bottom, 12)
        .frame(maxWidth: .infinity)
        .background(KColor.brand)
    }

    // MARK: - Contenido

    private var content: some View {
        VStack(spacing: 0) {
            if let next {
                NextClassCard(occurrence: next)
                    .padding(.top, -28)
            } else {
                NoClassesCard()
                    .padding(.top, -28)
            }

            QuickAccessRow()
                .padding(.top, 18)

            SectionHeader(title: "Tu semestre", action: "Ver semáforo")
                .padding(.top, 24)
            SemesterCard(progress: progress)
                .padding(.top, 12)

            if !rest.isEmpty {
                SectionHeader(title: "Resto del día", action: "Ver horario")
                    .padding(.top, 24)
                VStack(spacing: 10) {
                    ForEach(rest) { ClassRow(occurrence: $0) }
                }
                .padding(.top, 12)
            }
        }
        .padding(.horizontal, KMetrics.screenPadding)
        // Deja pasar la barra flotante sin que tape la última fila.
        .padding(.bottom, KMetrics.tabBarHeight + KMetrics.tabBarBottom + 24)
    }
}

// MARK: - Piezas

struct NextClassCard: View {
    let occurrence: ClassOccurrence

    var body: some View {
        HStack(spacing: 0) {
            // El riel lleva el color que mandó el servidor.
            Color(apiHex: occurrence.color)
                .frame(width: 6)

            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("PRÓXIMA CLASE")
                        .font(KType.eyebrow)
                        .tracking(0.9)
                        .foregroundStyle(KColor.textSoft)
                    Spacer()
                    Text("Empieza en 25 min")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(KColor.brand)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 4)
                        .background(KColor.green.opacity(0.32))
                        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
                }

                Text(occurrence.courseName)
                    .font(KType.subject)
                    .foregroundStyle(KColor.text)
                    .padding(.top, 9)

                Text("\(occurrence.startTime) – \(occurrence.endTime) · \(occurrence.placeLine)")
                    .font(.system(size: 14))
                    .foregroundStyle(KColor.textSoft)
                    .padding(.top, 3)

                Button { } label: {
                    HStack(spacing: 7) {
                        Image(systemName: "mappin.and.ellipse")
                        Text("Cómo llegar").font(KType.rowTitle)
                    }
                    .foregroundStyle(KColor.onAction)
                    .padding(.horizontal, 18)
                    .frame(height: KMetrics.buttonHeight)
                    .background(KColor.action)
                    .clipShape(RoundedRectangle(cornerRadius: KMetrics.buttonRadius, style: .continuous))
                }
                .buttonStyle(.plain)
                .padding(.top, 14)
            }
            .padding(16)
        }
        .fixedSize(horizontal: false, vertical: true)
        .background(KColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KMetrics.cardRadius, style: .continuous))
        .shadow(color: KColor.purple.opacity(0.16), radius: 14, x: 0, y: 10)
    }
}

/// `GET /api/schedule/me/day` devolvió `[]`: hoy no hay clases. No es un error.
struct NoClassesCard: View {
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "lightbulb")
                .font(.system(size: 22))
                .foregroundStyle(KColor.brand)
                .frame(width: 48, height: 48)
                .background(KColor.green.opacity(0.30))
                .clipShape(Circle())
            Text("Hoy no tienes clases")
                .font(KType.section)
                .foregroundStyle(KColor.text)
            Button("Ver la semana") { }
                .font(KType.body)
                .tint(KColor.action)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 168)
        .background(KColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KMetrics.cardRadius, style: .continuous))
        .shadow(color: KColor.purple.opacity(0.16), radius: 14, x: 0, y: 10)
    }
}

struct QuickAccessRow: View {
    /// Un color de la K por destino. El rosa no está: es de las acciones.
    private let tiles: [(String, String, Color)] = [
        ("Semáforo", "square.grid.2x2", KColor.forest),
        ("Horario", "calendar", KColor.blue),
        ("Mapa", "mappin.and.ellipse", KColor.purple),
        ("Perfil", "person", KColor.brown)
    ]

    var body: some View {
        HStack(spacing: 12) {
            ForEach(tiles, id: \.0) { title, symbol, tint in
                VStack(spacing: 8) {
                    Image(systemName: symbol)
                        .font(.system(size: 20))
                        .foregroundStyle(tint)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(tint.opacity(0.13))
                        .clipShape(RoundedRectangle(cornerRadius: KMetrics.tileRadius, style: .continuous))
                    Text(title)
                        .font(KType.label)
                        .foregroundStyle(KColor.text)
                }
            }
        }
    }
}

struct SectionHeader: View {
    let title: String
    let action: String

    var body: some View {
        HStack {
            Text(title)
                .font(KType.section)
                .foregroundStyle(KColor.text)
            Spacer()
            Button(action) { }
                .font(KType.link)
                .tint(KColor.action)
        }
    }
}

struct SemesterCard: View {
    let progress: ProgressSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Semestre \(progress.currentLevel)")
                        .font(KType.rowTitle)
                        .foregroundStyle(KColor.text)
                    Text("5 materias en curso")
                        .font(KType.meta)
                        .foregroundStyle(KColor.textSoft)
                }
                Spacer()
                Text("\(Int(progress.percentComplete.rounded()))%")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(KColor.brand)
            }

            GeometryReader { geo in
                HStack(spacing: 0) {
                    KColor.passed.frame(width: geo.size.width * progress.passedFraction)
                    KColor.inProgress.frame(width: geo.size.width * progress.inProgressFraction)
                    KColor.pending
                }
            }
            .frame(height: 10)
            .clipShape(Capsule())
            .padding(.top, 13)

            HStack(spacing: 14) {
                legend(KColor.passed, "\(progress.creditsPassed) aprobados")
                legend(KColor.inProgress, "\(progress.creditsInProgress) en curso")
                legend(KColor.pending, "\(progress.creditsRemaining) restantes")
            }
            .padding(.top, 13)
        }
        .padding(KMetrics.cardPadding)
        .kCard()
    }

    private func legend(_ color: Color, _ text: String) -> some View {
        HStack(spacing: 6) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(text)
                .font(KType.label)
                .foregroundStyle(KColor.textSoft)
        }
    }
}

struct ClassRow: View {
    let occurrence: ClassOccurrence

    var body: some View {
        HStack(spacing: 0) {
            Color(apiHex: occurrence.color)
                .frame(width: 4)

            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 1) {
                    Text(occurrence.startTime)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(KColor.text)
                    Text(occurrence.endTime)
                        .font(KType.label)
                        .foregroundStyle(KColor.textFaint)
                }
                .frame(width: 42, alignment: .leading)

                VStack(alignment: .leading, spacing: 2) {
                    Text(occurrence.courseName)
                        .font(KType.rowTitle)
                        .foregroundStyle(KColor.text)
                    Text(occurrence.placeLine)
                        .font(KType.label)
                        .foregroundStyle(KColor.textSoft)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
        }
        .frame(height: KMetrics.rowHeight)
        .kCard(radius: KMetrics.rowRadius)
    }
}

#Preview {
    HomeView()
}
