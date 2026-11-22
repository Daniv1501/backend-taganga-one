-- ============================================================
-- V7: Actualiza las preguntas frecuentes del asistente que venían del
-- prototipo y ya no coincidían con el mapa ZEPAT (V6) ni con el Plan de
-- Vida del Cabildo (colores del mapa viejo, "Capilla de San Pedro",
-- "reserva en rojo punteado", origen tayrona). Las palabras clave no cambian.
-- ============================================================

UPDATE assistant_entries SET answer = $$<p>Los sitios sagrados de Taganga hacen parte de la <b>Línea Negra</b>, el territorio ancestral que comparte con los pueblos de la Sierra Nevada. Algunos de los que se pueden nombrar públicamente:</p>
<ul><li><b>Isla de la Aguja (Ñiulalue):</b> donde los mamos de la Sierra hacen pagamento.</li>
<li><b>Dumbira:</b> ojo de agua sagrado; a su entrada está Murundúa, el árbol que abre el entendimiento.</li>
<li><b>Julaken:</b> el cerro detrás de la entrada al pueblo.</li>
<li><b>El cementerio</b>, a la entrada del pueblo.</li></ul>
<p>En todos: silencio, no extraer piedras ni plantas, no dejar ofrendas ajenas a la tradición y pedir permiso antes de fotografiar. Lee sus historias en la sección <b>Historias</b>.</p>$$
WHERE code = 'sagrados';

UPDATE assistant_entries SET answer = $$<p>Los Taganga son herederos de los <b>Peibuni Tuxe</b>, un pueblo caribe del litoral que intercambiaba con los pueblos de la Sierra Nevada. Por siglos su vida ha girado en torno a la <b>pesca con chinchorro</b> y a la relación espiritual con el mar y los cerros.</p>
<p>En 1873 protocolizaron sus tierras con la Escritura N.° 27 de San Antonio de Bonito Gordo, y el 5 de febrero de 2020 el Ministerio del Interior los reconoció como <b>Comunidad Indígena de Taganga</b>. Taganga One nace del Cabildo Indígena para proteger ese territorio.</p>$$
WHERE code = 'historia';

UPDATE assistant_entries SET answer = $$<p>Son las <b>Zonas Especiales de Pesca Ancestral (ZEPAT)</b>: playas y ancones donde tradicionalmente se pesca con chinchorro. En el mapa aparecen <b>achuradas en azul</b>, y los colores encima muestran qué actividades afectan esa pesca.</p>
<ul><li>Si el chinchorro está tendido, no entres nadando ni en embarcación.</li>
<li>No ancles ni bucees en las áreas marcadas con afectación o como "No buceo".</li>
<li>Motos acuáticas, gusano y kayak deben seguir sus recorridos, lejos de los ancones en faena.</li>
<li>No manipules redes, boyas ni nasas.</li></ul>$$
WHERE code = 'pesca';

UPDATE assistant_entries SET answer = $$<p>Hay dos formas:</p>
<ul><li><b>A pie:</b> por el sendero costero que sale del extremo sur del malecón (unos 20–30 min). Ve con agua, calzado cerrado y no salgas del camino.</li>
<li><b>En lancha:</b> desde la playa principal, con lancheros locales.</li></ul>
<p>En el mapa, Playa Grande está junto a la <b>ZEPAT Lacón – Sisicuaca – Monocuaca</b>: si ves un chinchorro tendido, no entres a esa área.</p>$$
WHERE code = 'llegar';
