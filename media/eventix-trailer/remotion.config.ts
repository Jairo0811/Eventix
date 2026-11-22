import {Config} from '@remotion/cli/config';

/**
 * Los recursos del tráiler viven en assets/, no en public/: así la estructura
 * del proyecto refleja su contenido (marca, capturas, audio) en lugar de
 * esconderlo todo tras un nombre genérico. staticFile() los resuelve desde ahí.
 */
Config.setPublicDir('assets');
Config.setVideoImageFormat('jpeg');
Config.setChromiumOpenGlRenderer('angle');
