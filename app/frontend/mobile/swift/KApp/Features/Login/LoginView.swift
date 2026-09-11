import SwiftUI

/// En qué punto está el intento de entrar.
///
/// Los tres errores son distintos a propósito y salen del contrato en `docs/api/auth.openapi.yaml`:
/// un 401 no dice si la cuenta existe, un 403 no es un error de credenciales sino una tarea
/// pendiente, y quedarse sin red no es culpa de lo que el estudiante escribió.
enum LoginState: Equatable {
    case idle
    case sending
    /// 401 — un solo mensaje para correo desconocido y contraseña mala.
    case invalidCredentials
    /// 403 — la contraseña era correcta; falta confirmar el correo.
    case unverified(email: String)
    case offline
}

struct LoginView: View {
    var onSignedIn: () -> Void

    @State private var user = ""
    @State private var password = ""
    @State private var staySignedIn = true
    @State private var revealPassword = false
    @State private var state: LoginState = .idle

    /// Solo se escribe el usuario; el dominio lo pone la app. Si alguien pega el correo
    /// completo, no se duplica.
    private var email: String {
        let trimmed = user.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.contains("@") ? trimmed : "\(trimmed)@konradlorenz.edu.co"
    }

    private var canSubmit: Bool {
        !user.trimmingCharacters(in: .whitespaces).isEmpty
            && !password.isEmpty
            && state != .sending
    }

    var body: some View {
        ZStack(alignment: .top) {
            KColor.brand.ignoresSafeArea()

            VStack(spacing: 0) {
                brandField
                sheet
            }
            .ignoresSafeArea(.container, edges: .bottom)
        }
    }

    // MARK: - Campo de marca

    private var brandField: some View {
        VStack(spacing: 16) {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(KColor.surface)
                .frame(width: 84, height: 84)
                .overlay(
                    Image("KonradLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 60, height: 60)
                )
                .shadow(color: .black.opacity(0.18), radius: 12, x: 0, y: 8)

            VStack(spacing: 4) {
                Text("KApp")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(.white)
                Text("Fundación Universitaria Konrad Lorenz")
                    .font(KType.body)
                    .foregroundStyle(.white.opacity(0.74))
            }
        }
        .padding(.top, 30)
        .padding(.bottom, 34)
        .frame(maxWidth: .infinity)
    }

    // MARK: - Hoja del formulario

    private var sheet: some View {
        VStack(spacing: 0) {
            KTricolorEdge()

            VStack(alignment: .leading, spacing: 20) {
                emailField
                passwordField

                Toggle(isOn: $staySignedIn) {
                    Text("Mantener la sesión iniciada")
                        .font(.system(size: 16))
                        .foregroundStyle(KColor.text)
                }
                .tint(KColor.action)
                .frame(minHeight: 44)

                if let message = errorMessage {
                    errorRow(message)
                }

                if case .unverified(let address) = state {
                    verificationNotice(address)
                }

                submitButton

                HStack(spacing: 5) {
                    Text("¿Primera vez?")
                        .font(KType.body)
                        .foregroundStyle(KColor.textSoft)
                    Button("Usa tu código de invitación") { }
                        .font(.system(size: 15, weight: .semibold))
                        .tint(KColor.action)
                }
                .frame(maxWidth: .infinity)

                Spacer(minLength: 0)

                Text("© 2026 Fundación Universitaria Konrad Lorenz")
                    .font(.system(size: 11))
                    .foregroundStyle(KColor.textFaint)
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 28)
            }
            .padding(.horizontal, 24)
            .padding(.top, 32)
        }
        .background(KColor.surface)
        .clipShape(UnevenRoundedRectangle(topLeadingRadius: 28, topTrailingRadius: 28, style: .continuous))
    }

    private var emailField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Correo institucional")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(KColor.textSoft)

            HStack(spacing: 0) {
                TextField("pepito.perez", text: $user)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.emailAddress)
                    .font(.system(size: 16))
                    .foregroundStyle(KColor.text)

                // El dominio se muestra fijo: explica el autocompletado sin una línea de ayuda.
                if !user.contains("@") {
                    Text("@konradlorenz.edu.co")
                        .font(.system(size: 16))
                        .foregroundStyle(KColor.textFaint)
                }
            }
            .padding(.horizontal, 16)
            .frame(height: KMetrics.fieldHeight)
            .background(fieldBackground(invalid: state == .invalidCredentials))
        }
    }

    private var passwordField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Contraseña")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(KColor.textSoft)

            HStack {
                Group {
                    if revealPassword {
                        TextField("", text: $password)
                    } else {
                        SecureField("", text: $password)
                    }
                }
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .font(.system(size: 16))
                .foregroundStyle(KColor.text)

                Button {
                    revealPassword.toggle()
                } label: {
                    Image(systemName: revealPassword ? "eye.slash" : "eye")
                        .foregroundStyle(KColor.textSoft)
                }
                .buttonStyle(.plain)
                .frame(width: 44, height: 44)
            }
            .padding(.leading, 16)
            .padding(.trailing, 4)
            .frame(height: KMetrics.fieldHeight)
            .background(fieldBackground(invalid: state == .invalidCredentials))
        }
    }

    private func fieldBackground(invalid: Bool) -> some View {
        RoundedRectangle(cornerRadius: KMetrics.fieldRadius, style: .continuous)
            .fill(invalid ? KColor.surface : KColor.background)
            .overlay(
                RoundedRectangle(cornerRadius: KMetrics.fieldRadius, style: .continuous)
                    .stroke(invalid ? KColor.error : KColor.border, lineWidth: invalid ? 1.5 : 1)
            )
    }

    private var errorMessage: String? {
        switch state {
        case .invalidCredentials: "Revisa tu correo o tu contraseña e inténtalo de nuevo."
        case .offline: "No pudimos conectar con el servidor. Revisa tu conexión e inténtalo otra vez."
        default: nil
        }
    }

    private func errorRow(_ message: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "exclamationmark.circle")
                .foregroundStyle(KColor.error)
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(KColor.error)
        }
    }

    private func verificationNotice(_ address: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 9) {
                Image(systemName: "envelope")
                    .foregroundStyle(KColor.brand)
                Text("Confirma tu correo")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(KColor.brand)
            }
            Text("Te enviamos un enlace a \(address). Ábrelo y vuelve a entrar.")
                .font(.system(size: 14))
                .foregroundStyle(KColor.text.opacity(0.8))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(KColor.brand.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: KMetrics.rowRadius, style: .continuous))
    }

    private var submitButton: some View {
        Button {
            submit()
        } label: {
            HStack(spacing: 10) {
                if state == .sending {
                    ProgressView().tint(KColor.onAction)
                }
                Text(buttonTitle)
                    .font(.system(size: 17, weight: .semibold))
            }
            .foregroundStyle(KColor.onAction)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(KColor.action.opacity(canSubmit ? 1 : 0.55))
            .clipShape(RoundedRectangle(cornerRadius: KMetrics.buttonRadius, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!canSubmit)
    }

    private var buttonTitle: String {
        switch state {
        case .sending: "Ingresando…"
        case .unverified: "Reenviar correo"
        default: "Ingresar"
        }
    }

    /// Todavía no habla con el gateway. Cuando exista el cliente de red, esto llama a
    /// `POST /auth/login` con `email` y `password`, y mapea 401 → `.invalidCredentials`,
    /// 403 → `.unverified`, y cualquier fallo de transporte → `.offline`.
    private func submit() {
        state = .sending
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            state = .idle
            onSignedIn()
        }
    }
}

#Preview {
    LoginView(onSignedIn: {})
}
