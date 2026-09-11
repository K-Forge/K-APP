import Foundation

/// Una clase que sí ocurre en una fecha concreta.
///
/// Espejo de `ClassOccurrence` en `docs/api/schedule.openapi.yaml`. El inicio la pide con
/// `GET /api/schedule/me/day?date=…`, que el contrato marca como *el* endpoint de esta
/// pantalla. `room` puede venir nulo: hay rangos del semestre sin salón asignado.
struct ClassOccurrence: Codable, Identifiable, Equatable {
    let enrollmentId: String
    let courseCode: String
    let courseName: String
    let professor: String?
    let startTime: String
    let endTime: String
    let room: String?
    let campus: String?
    /// Lo manda el servidor. El cliente no elige el color de una materia: lo pinta.
    let color: String?

    var id: String { enrollmentId }

    /// "Salón 401 · Bloque B", o solo la sede cuando todavía no hay salón.
    var placeLine: String {
        [room.map { "Salón \($0)" }, campus]
            .compactMap { $0 }
            .joined(separator: " · ")
    }
}

/// Créditos aprobados, en curso y restantes.
///
/// Espejo de `ProgressSummary` en `docs/api/semaphore.openapi.yaml`. El servidor lo recalcula
/// en cada llamada, nunca lo guarda, así que no puede desfasarse del semáforo que el
/// estudiante está viendo. `percentComplete` es aprobados sobre el total: los que están en
/// curso todavía no cuentan.
struct ProgressSummary: Codable, Equatable {
    let creditsPassed: Int
    let creditsInProgress: Int
    let creditsRemaining: Int
    let totalCredits: Int
    let percentComplete: Double
    let currentLevel: Int

    var passedFraction: Double {
        totalCredits > 0 ? Double(creditsPassed) / Double(totalCredits) : 0
    }

    var inProgressFraction: Double {
        totalCredits > 0 ? Double(creditsInProgress) / Double(totalCredits) : 0
    }
}

// MARK: - Datos de ejemplo

/// Lo que se ve mientras no exista la capa de red. Son los mismos números de la maqueta y
/// cuadran entre sí: 112 + 15 + 15 = 142.
enum SampleData {
    static let today: [ClassOccurrence] = [
        ClassOccurrence(
            enrollmentId: "5b8e2a41-6c07-4f3d-8e19-a2d7c4b90f63",
            courseCode: "17080",
            courseName: "Bases de Datos",
            professor: "CAMPOS AVENDANO GUSTAVO ANDRES",
            startTime: "8:00", endTime: "10:00",
            room: "401", campus: "Bloque B",
            color: "#539392"
        ),
        ClassOccurrence(
            enrollmentId: "7c1f9d02-3b55-4a88-91de-6f0c2e7b1a44",
            courseCode: "17112",
            courseName: "Arquitectura de SW",
            professor: nil,
            startTime: "11:00", endTime: "13:00",
            room: "302", campus: "Bloque A",
            color: "#522567"
        ),
        ClassOccurrence(
            enrollmentId: "9a4d6e18-0c73-4f21-b6a5-8d3e1c0f2b77",
            courseCode: "17140",
            courseName: "Redes II",
            professor: nil,
            startTime: "14:00", endTime: "16:00",
            room: "Lab 105", campus: "Bloque C",
            color: "#592E2A"
        )
    ]

    static let progress = ProgressSummary(
        creditsPassed: 112,
        creditsInProgress: 15,
        creditsRemaining: 15,
        totalCredits: 142,
        percentComplete: 78.9,
        currentLevel: 8
    )
}
