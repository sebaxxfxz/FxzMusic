# Limpieza agresiva propuesta (pendiente de tu OK)

Esta es la lista exacta detectada como candidata a borrado/refactor **antes de eliminar**.

## Candidatos de archivos

1. `app/src/main/java/com/example/fxzmusic/PlaylistDetail_Rediseño.kt`
   - Solo contiene `PlaylistDetailRedesignReference`.
   - No tiene referencias externas (0 usos).

## Candidatos de funciones no usadas

En `app/src/main/java/com/example/fxzmusic/AnimationUtils.kt`:

1. `slideInFromTopWithRotation`
2. `rotateInFromRight`
3. `rotateOutToLeft`
4. `slideInFromLeftWithDelay`
5. `slideOutToLeft`
6. `FloatingTextAnimation`
7. `PulseAnimation`
8. `ShimmerAnimation`
9. `GlitchTextAnimation`
10. `ContentTransformSlideBottom`
11. `ContentTransformSlideHorizontal`
12. `ContentTransformScale`

Notas:
- La función `slideInFromBottomWithScale` sí se usa en `HomeScreen.kt` y `AnimationUtils.kt`.
- Existe también otra sobrecarga en `CommonAnimations.kt`, por lo que propongo revisar juntas esas dos implementaciones antes de borrar una.

## Candidato no Kotlin

1. `build.gradle.kts.project`
       - Parece archivo auxiliar/backup.
   - No es parte estándar del build de Gradle.

## Plan de borrado seguro (si confirmas)

1. Borrar `PlaylistDetail_Rediseño.kt`.
2. Borrar las 12 funciones no usadas de `AnimationUtils.kt`.
3. Revalidar compilación con `:app:compileDebugKotlin`.
4. Revisar warnings y aplicar segunda pasada en archivos restantes.

