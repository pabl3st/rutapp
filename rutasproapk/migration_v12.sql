-- Migration v12: añadir campos de check-in/check-out y GPS de visita
ALTER TABLE stops ADD COLUMN check_in_ts BIGINT NULL COMMENT 'Epoch ms — cuando el agente abrió el formulario';
ALTER TABLE stops ADD COLUMN check_out_ts BIGINT NULL COMMENT 'Epoch ms — cuando se guardó la visita';
ALTER TABLE stops ADD COLUMN gps_lat_visit DOUBLE NULL COMMENT 'Latitud GPS del agente al momento del check-in';
ALTER TABLE stops ADD COLUMN gps_lng_visit DOUBLE NULL COMMENT 'Longitud GPS del agente al momento del check-in';
