-- ============================================================
-- V8: Las preguntas rápidas del asistente pasan a ser sobre el mapa actual
-- (cartografía ZEPAT, V6). Con Gemini configurado, el frontend envía estas
-- preguntas a la IA; la respuesta guardada aquí solo se usa sin conexión o
-- cuando Gemini no responde (motor de palabras clave).
--
-- Las preguntas del prototipo que la V7 ya corrigió dejan de mostrarse como
-- botones (pasan a EXTRA) y siguen sirviendo al motor de respaldo. La de
-- "reserva pesquera" se elimina: la reemplaza la de las zonas ZEPAT.
-- ============================================================

-- 1. Fuera la pregunta de reserva pesquera (primero sus palabras clave)
DELETE FROM assistant_keywords
WHERE entry_id = (SELECT id FROM assistant_entries WHERE code = 'pesca');
DELETE FROM assistant_entries WHERE code = 'pesca';

-- 2. Las preguntas del prototipo ya corregidas pasan a respuestas internas,
--    después de las respuestas adicionales existentes
UPDATE assistant_entries SET type = 'EXTRA', display_order = 20 WHERE code = 'sagrados';
UPDATE assistant_entries SET type = 'EXTRA', display_order = 21 WHERE code = 'ambiente';
UPDATE assistant_entries SET type = 'EXTRA', display_order = 22 WHERE code = 'historia';
UPDATE assistant_entries SET type = 'EXTRA', display_order = 23 WHERE code = 'datos';
UPDATE assistant_entries SET type = 'EXTRA', display_order = 24 WHERE code = 'llegar';

-- 3. El saludo presenta a Tagui y los temas nuevos
UPDATE assistant_entries
SET answer = $$<p>¡Hola{visitor}! 👋 Soy <b>Tagui</b>, tu guía en Taganga One. Pregúntame por las zonas del mapa, dónde bucear o hacer snorkel, o por las historias de Taganga.</p>$$
WHERE code = 'saludo';

-- 4. Nuevas preguntas rápidas sobre el mapa
INSERT INTO assistant_entries (code, type, question, answer, display_order) VALUES
('zepat', 'FAQ', '¿Qué son las zonas ZEPAT del mapa?',
 $$<p>Son las <b>Zonas Especiales de Pesca Ancestral (ZEPAT)</b>: playas y ancones donde tradicionalmente se pesca con chinchorro. En el mapa aparecen <b>achuradas en azul</b>, y los colores encima muestran qué actividades afectan esa pesca.</p>
<ul><li>Si el chinchorro está tendido, no entres nadando ni en embarcación.</li>
<li>No ancles ni bucees en las áreas marcadas con afectación o como "No buceo".</li>
<li>No manipules redes, boyas ni nasas.</li></ul>$$,
 1),
('buceo', 'FAQ', '¿Dónde puedo bucear?',
 $$<p>Depende de la zona. En el mapa, dentro de las <b>ZEPAT</b>:</p>
<ul><li><b>Buceo concertado (verde):</b> Rumbao y Granate. Es el buceo acordado con los pescadores.</li>
<li><b>No buceo (café):</b> costa occidental de Isla Aguja.</li>
<li><b>Buceo con afectación (naranja):</b> punta norte y costa sur de Isla Aguja, y Divijuca. Evítalo o consulta antes con el Cabildo.</li></ul>
<p>Bucea siempre con un operador local registrado y no toques los corales.</p>$$,
 2),
('snorkel', 'FAQ', '¿Dónde puedo hacer snorkel?',
 $$<p>En el mapa:</p>
<ul><li><b>Snorkel sin afectación (morado oscuro):</b> Lacón y Playita.</li>
<li><b>Snorkel con afectación (morado):</b> Playa Majagua. Evítalo mientras haya faenas de pesca.</li></ul>
<p>No toques ni pises los corales, no alimentes a los peces y usa protector solar biodegradable.</p>$$,
 3),
('recorridos', 'FAQ', '¿Por dónde van el kayak, el gusano y la moto acuática?',
 $$<p>Son las líneas de la bahía en el mapa (filtro <b>Recorridos</b>):</p>
<ul><li><b>Kayak</b> (azul) y <b>gusano inflable</b> (fucsia): se mueven entre Playa Grande y la playa del pueblo.</li>
<li><b>Moto acuática</b> (línea roja de puntos): a velocidad baja cerca de bañistas, pescadores y corales.</li></ul>
<p>Todos deben mantenerse lejos de los ancones cuando hay un chinchorro tendido.</p>$$,
 4),
('embarcaciones', 'FAQ', '¿Dónde no debo fondear una lancha?',
 $$<p>Evita fondear en las áreas <b>rojas</b> del mapa, donde las embarcaciones afectan la pesca: <b>Divijuca</b> y <b>Playa Majagua</b>.</p>
<ul><li>Navega despacio dentro de la bahía.</li>
<li>No ancles ni cruces sobre redes o boyas.</li>
<li>Las lanchas de transporte turístico siguen la <b>ruta Taganga – Tayrona</b> (línea cian).</li></ul>$$,
 5),
('historias-mapa', 'FAQ', '¿Qué son los marcadores de historias del mapa?',
 $$<p>Los marcadores con forma de gota señalan dónde ocurrieron las <b>Historias de Taganga</b>: sitios sagrados, ancones, autoridades espirituales y memoria del pueblo.</p>
<p>Toca uno y luego <b>Leer historia</b> para abrirla. Desde cada historia, <b>Ver en el mapa</b> te devuelve a su lugar. El filtro <b>Historias</b> los muestra u oculta.</p>$$,
 6);

INSERT INTO assistant_keywords (entry_id, sort_order, keyword)
SELECT e.id, k.ord - 1, k.keyword
FROM (VALUES
    ('zepat',          ARRAY['zepat','zona especial','achurad','ancon','ancón','chinchorro','pesca','pescad']),
    ('buceo',          ARRAY['buce','bucea','inmersi','tanque']),
    ('snorkel',        ARRAY['snorkel','esnorkel','careteo','caretear','careta','coral']),
    ('recorridos',     ARRAY['kayak','gusano','inflable','moto acu','jet','recorrido','bahía','bahia']),
    ('embarcaciones',  ARRAY['lancha','fondea','fondeo','ancla','embarcaci','bote','velero','transporte']),
    ('historias-mapa', ARRAY['historia','marcador','leyenda','relato','cuento','autoridad'])
) AS v (code, keywords)
CROSS JOIN LATERAL unnest(v.keywords) WITH ORDINALITY AS k (keyword, ord)
JOIN assistant_entries e ON e.code = v.code;
