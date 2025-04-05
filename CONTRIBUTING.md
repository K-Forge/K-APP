# 🛠️ Guía para Contribuir a K-APP

## 🤝 ¿Quién puede contribuir?
Este proyecto es parte de K-Forge y está restringido a miembros autorizados.  
Si formas parte del equipo de desarrollo, sigue estas pautas para contribuir al código.

---

## 📌 Convención para Commits
Para mantener un historial limpio y comprensible, seguimos la convención **Conventional Commits**. Usa el siguiente formato:

```
tipo(scope): mensaje corto en presente
```

---

### 🧾 Tipos de Commits:
El *tipo* indica la naturaleza del cambio realizado. Cada tipo ayuda a identificar rápidamente el propósito del commit en el historial del repositorio.
- **feat**: Nueva funcionalidad  
- **fix**: Corrección de errores  
- **docs**: Cambios en documentación  
- **style**: Cambios que no afectan la funcionalidad (espacios, comas, etc.)  
- **refactor**: Mejoras en el código sin cambiar funcionalidad  
- **test**: Agregar o modificar tests  
- **chore**: Tareas de mantenimiento del proyecto  

---

### 🗂️ Scopes Comunes:
El *scope* indica qué parte del código se está modificando.
- **ui**: interfaz de usuario  
- **auth**: autenticación y manejo de sesión  
- **db**: base de datos  
- **api**: servicios o controladores  
- **readme**, **contributing**, **license**: archivos del repositorio  
- **config**: configuración del proyecto  

Puedes proponer nuevos scopes si se justifica por el contexto del cambio.

---

### ✅ Ejemplos Correctos:
- feat(ui): agregar pantalla de inicio  
- fix(auth): corregir bug en validación de usuario  
- docs(readme): actualizar instrucciones de instalación  
- refactor(db): optimizar consultas de obtención de usuarios  

---

### ⛔ Ejemplos Incorrectos:
- `update` → No describe nada útil  
- `cambios` → Muy ambiguo  
- `arreglos varios` → No es claro qué se hizo  

---

📚 Para más información, visita [Conventional Commits](https://www.conventionalcommits.org/)