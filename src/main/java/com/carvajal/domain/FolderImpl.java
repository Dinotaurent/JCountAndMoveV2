package com.carvajal.domain;

import java.io.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.*;
import org.apache.log4j.Logger;

/**
 *
 * @author dandazme
 */
public class FolderImpl implements IFolder {

    private static final Logger log = Logger.getLogger(FolderImpl.class);
    private static final String PATH_ENTRADA = "C:\\Users\\danie\\Desktop\\prueba";
    private static final String PATH_TEMP = "C:\\Users\\danie\\Desktop\\prueba\\temp\\";
    public int contadorEntrada = 0;
    public int contadorTemp = 0;
    public int contadorAnterior = 0;
    public File[] listadoEntrada;
    public File[] listadoTemp;
    public ArrayList<String> nombresArchivosEntrada = new ArrayList<String>();
    public ArrayList<String> nombresArchivosTemp = new ArrayList<String>();
    public ArrayList<String> dosificador = new ArrayList<String>();
    public ArrayList<String> docNuevos = new ArrayList<String>();
    public ArrayList<String> documentos = new ArrayList<String>();
    public ArrayList<String> nombresDocNuevos = new ArrayList<String>();
    public ArrayList<String> docRezagados = new ArrayList<String>();
    public boolean rezagado = false;
    public boolean movido = true;
    public boolean bandera = false;
    public boolean renombrado = false;
    File pathEntrada = new File(PATH_ENTRADA);
    File pathTemp = new File(PATH_TEMP);
    BasicFileAttributes attrs;

    FileFilter filtro = (File file) -> !file.isHidden() && file.getName().endsWith(".txt");

    @Override
    public void contar() {
        listadoEntrada = pathEntrada.listFiles(filtro);
        listadoTemp = pathTemp.listFiles(filtro);
        contadorEntrada = listadoEntrada.length;
        contadorTemp = listadoTemp.length;
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyyHHmmss");
        if (listadoEntrada != null || listadoEntrada.length != 0) {

            log.info("Se encontraron: " + contadorEntrada + " documentos en la ruta de entrada");
            for (File archivo : listadoEntrada) {
                try {
                    attrs = Files.readAttributes(archivo.toPath(), BasicFileAttributes.class);
                    FileTime time = attrs.creationTime();
                    nombresArchivosEntrada.add(sdf.format(new Date(time.toMillis())) + archivo.getName());
                    nombresDocNuevos.add(sdf.format(new Date(time.toMillis())) + archivo.getName());
                } catch (IOException ex) {
                    log.error(ex);
                }
            }
            Collections.sort(nombresArchivosEntrada);
        }

        if (listadoTemp != null || listadoTemp.length != 0) {
            for (File archivo : listadoTemp) {
                try {
                    attrs = Files.readAttributes(archivo.toPath(), BasicFileAttributes.class);
                    FileTime time = attrs.creationTime();
                    nombresArchivosTemp.add(sdf.format(new Date(time.toMillis())) + archivo.getName());
                } catch (IOException ex) {
                    log.error(ex);
                }
            }
            Collections.sort(nombresArchivosTemp);

            if (!nombresArchivosTemp.isEmpty()) {
                if (nombresArchivosTemp.size() < 5) {
                    for (int i = 0; dosificador.size() < contadorTemp; i++) {
                        dosificador.add(nombresArchivosTemp.get(i));
                    }
                } else if (nombresArchivosTemp.size() >= 5) {
                    for (int i = 0; dosificador.size() < 5; i++) {
                        dosificador.add(nombresArchivosTemp.get(i));
                    }
                }
            }
        }
        if (docRezagados.isEmpty()) {
            for (int i = 0; docRezagados.size() < contadorEntrada; i++) {
                docRezagados.add(nombresArchivosEntrada.get(i));
            }
        } else {
            docRezagados.clear();
        }

        mover();
    }

    @Override
    public void mover() {

        if (contadorEntrada > 5 && contadorTemp == 0) {
//            System.out.println("Entro en la secuencia: A");
            log.info("Se ha detectado bloqueo, procedera a mover los documentos y reiniciar los servicios");
            try {
                //Se recorren los nombres y se elimina la fecha del nombre.
                for (String archivo : nombresArchivosEntrada) {
                    StringBuilder sb = new StringBuilder(archivo);
                    for (int i = 0; i < 14; i++) {
                        sb.deleteCharAt(0);
                    }
                    documentos.add(String.valueOf(sb));
                }
                //Se recorren los documentos y se crea un path con los nombres para ser movidos.
                for (String archivo : documentos) {
                    Path documento = Paths.get(PATH_ENTRADA + "\\" + archivo);
                    Path destino = Paths.get(PATH_TEMP + archivo);
//                System.out.println(documento);
                    try {
//                        Path mover = Files.move(documento, destino);
                        Path mover = Files.move(documento, destino.resolveSibling(destino));
                        log.info("Se movio el archivo " + documento + " a la ruta: " + PATH_TEMP);

                    } catch (Exception ex) {
                        log.error(ex);
                        while (renombrado == false) {
//                            System.out.println("El archivo no se puede mover debido que esta en uso, se esperar 10 segundos");
                            log.info("El archivo no se puede mover debido a que esta en uso, se  volvera a intentar en 10 segundos");
                            Thread.sleep(10000);
                            if (renombrado == false) {
                                try {
                                    Path mover = Files.move(documento, destino.resolveSibling(destino));
                                    renombrado = true;
                                } catch (Exception exx) {
                                    log.error(exx);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.error(ex);
            }

            //Se reinician los servicios.
            try {
                Thread.sleep(3000);
                String[] cmd = {"sc.exe stop ServicioTestJavaV2", "sc.exe config \"ServicioTestJavaV2\" obj= \".\\usuario2\" password= \",,41qw96\"", "sc.exe start ServicioTestJavaV2"};

                for (String i : cmd) {
                    Runtime.getRuntime().exec(i);
                    Thread.sleep(2000);
                }

                log.info("Se han reiniciado los servicios correctamente!!");
            } catch (IOException | InterruptedException ex) {
                log.error(ex);
            }

            //Se limpian todos los arreglos para evitar sobreescritura.
            renombrado = false;
            documentos.clear();
            docNuevos.clear();
            dosificador.clear();
            nombresArchivosTemp.clear();
            nombresArchivosEntrada.clear();
            nombresDocNuevos.clear();
            docRezagados.clear();
            contar();
        } else if (contadorEntrada == 0 && contadorTemp > 0) {
//            System.out.println("Entro en la secuencia: B");
            log.info("Se procedera a dosificar los documentos a la ruta de entrada");
            try {
                //Se recorren los nombres y se elimina la fecha del nombre.
                for (String archivo : dosificador) {
                    StringBuilder sb = new StringBuilder(archivo);
                    for (int i = 0; i < 14; i++) {
                        sb.deleteCharAt(0);
                    }
                    documentos.add(String.valueOf(sb));
                }
                //Se recorren los documentos y se crea un path con los nombres para ser movidos.
                for (String archivo : documentos) {
                    Path documento = Paths.get(PATH_TEMP + archivo);
                    Path destino = Paths.get(PATH_ENTRADA + "\\" + archivo);

                    try {
                        Path mover = Files.move(documento, destino);
                        log.info("Se movio el archivo " + documento + " a la ruta: " + PATH_ENTRADA);
                    } catch (Exception ex) {
                        log.error(ex);
                    }
                }

            } catch (Exception ex) {
                log.error(ex);
            }

            //Se limpian todos los arreglos para evitar sobreescritura.
            renombrado = false;
            documentos.clear();
            docNuevos.clear();
            dosificador.clear();
            nombresArchivosTemp.clear();
            nombresArchivosEntrada.clear();
            nombresDocNuevos.clear();
            docRezagados.clear();
            contar();

        } else if (contadorEntrada == 0 && contadorTemp == 0 || contadorEntrada <= 5 && contadorTemp == 0 && !rezagado) {
//            System.out.println("Entro en la secuencia: C");
            log.info("No se detectaron bloqueos ni documentos pendientes o la cantidad es inferior al limite, se volvera a ejecutar dentro de 2 minutos");
            try {

                if (!movido) {
                    rezagado = true;
                }

                if (docRezagados.equals(nombresArchivosEntrada)) {
                    movido = false;
                }

                Thread.sleep(120000);
            } catch (InterruptedException ex) {
                log.error(ex);
            }

            //Se limpian todos los arreglos para evitar sobreescritura.
            renombrado = false;
            documentos.clear();
            docNuevos.clear();
            dosificador.clear();
            nombresArchivosTemp.clear();
            nombresArchivosEntrada.clear();
            nombresDocNuevos.clear();
            contar();
        } else if (contadorEntrada <= 5 && contadorTemp > 0 && !rezagado) {
            log.info("Aun no se han evacuado los documentos dosificados, se volvera a ejecutar dentro de 1 minuto");
            try {

                if (bandera && contadorEntrada == 5 || bandera && movido) {
                    rezagado = true;
                } else if (bandera && contadorEntrada < 5) {
                    movido = true;
                }

                if (!movido && contadorEntrada == 5) {
                    bandera = true;
                } else if (!movido && contadorAnterior == contadorEntrada) {
                    rezagado = true;
                }

                if (!rezagado) {
                    if (contadorEntrada == 5) {
                        if (docRezagados.equals(nombresArchivosEntrada)) {
                            movido = false;
                        }
                    } else if (contadorEntrada <= 5) {
                        if (contadorAnterior == 0 || contadorAnterior != contadorEntrada) {
                            contadorAnterior = contadorEntrada;
                        } else if (contadorAnterior == contadorEntrada) {
                            movido = false;
                        }
                    }
                }
                Thread.sleep(90000);
            } catch (InterruptedException ex) {
                log.error(ex);
            }

            //Se limpian todos los arreglos para evitar sobreescritura.
            renombrado = false;
            documentos.clear();
            docNuevos.clear();
            dosificador.clear();
            nombresArchivosTemp.clear();
            nombresArchivosEntrada.clear();
            nombresDocNuevos.clear();
            docRezagados.clear();
            contar();
        } else if (contadorEntrada > 5 && contadorTemp > 0) {
//            System.out.println("Entre en la secuencia: E");
            log.info("Se encontraron nuevos documentos que bloquearian en servicio, se procedera a mover a la ruta temporal");
            //Se ordena los nombres de todos los docs de la ruta de entrada.
            Collections.sort(nombresDocNuevos);
            try {
                //Se agregan a docNuevos los nombres de los documentos nuevos.
                for (int i = 5; i < contadorEntrada; i++) {
                    docNuevos.add(nombresDocNuevos.get(i));
                }
                //Se recorren los nombres y se elimina la fecha del nombre.
                for (String archivo : docNuevos) {
                    StringBuilder sb = new StringBuilder(archivo);
                    for (int i = 0; i < 14; i++) {
                        sb.deleteCharAt(0);
                    }
                    documentos.add(String.valueOf(sb));
                }
                //Se recorren los documentos y se crea un path con los nombres para ser movidos.
                for (String archivo : documentos) {
                    Path documento = Paths.get(PATH_ENTRADA + "\\" + archivo);
                    Path destino = Paths.get(PATH_TEMP + archivo);
                    try {
                        Path mover = Files.move(documento, destino.resolveSibling(destino));
                        log.info("Se movio el archivo " + documento + " a la ruta: " + PATH_TEMP);
                    } catch (Exception ex) {
                        log.error(ex);
                        while (renombrado == false) {
                            log.info("El archivo no se puede mover debido a que esta en uso, se  volvera a intentar en 10 segundos");
                            Thread.sleep(10000);
                            if (renombrado == false) {
                                try {
                                    Path mover = Files.move(documento, destino.resolveSibling(destino));
                                    renombrado = true;
                                } catch (Exception exx) {
                                    log.error(exx);
                                }
                            }
                        }
                    }
                }
                Thread.sleep(3000);
            } catch (Exception ex) {
                log.error(ex);
            }

            //Se limpian todos los arreglos para evitar sobreescritura.
            renombrado = false;
            documentos.clear();
            docNuevos.clear();
            dosificador.clear();
            nombresArchivosTemp.clear();
            nombresArchivosEntrada.clear();
            nombresDocNuevos.clear();
            docRezagados.clear();
            contar();
        } else if (contadorEntrada <= 5 && rezagado) {
//            System.out.println("Entre en la secuencia: F");
            log.info("No se detecto movimiento en la ruta de entrada dentro de mucho tiempo, se procedera a reiniciar los servicios");
            try {
                //Se recorren los nombres y se elimina la fecha del nombre.
                for (String archivo : nombresArchivosEntrada) {
                    StringBuilder sb = new StringBuilder(archivo);
                    for (int i = 0; i < 14; i++) {
                        sb.deleteCharAt(0);
                    }
                    documentos.add(String.valueOf(sb));
                }
                //Se recorren los documentos y se crea un path con los nombres para ser movidos.
                for (String archivo : documentos) {
                    Path documento = Paths.get(PATH_ENTRADA + "\\" + archivo);
                    Path destino = Paths.get(PATH_TEMP + archivo);
//                System.out.println(documento);
                    try {
                        Path mover = Files.move(documento, destino.resolveSibling(destino));
                        log.info("Se movio el archivo " + documento + " a la ruta: " + PATH_TEMP);
                    } catch (Exception ex) {
                        log.error(ex);
                        while (renombrado == false) {
                            log.info("El archivo no se puede mover debido a que esta en uso, se  volvera a intentar en 10 segundos");
                            Thread.sleep(10000);
                            if (renombrado == false) {
                                try {
                                    Path mover = Files.move(documento, destino.resolveSibling(destino));
                                    renombrado = true;
                                } catch (Exception exx) {
                                    log.error(exx);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.error(ex);
            }

            //Se reinician los servicios.
            try {
                Thread.sleep(3000);
                String[] cmd = {"sc.exe stop ServicioTestJavaV2", "sc.exe config \"ServicioTestJavaV2\" obj= \".\\usuario2\" password= \",,41qw96\"", "sc.exe start ServicioTestJavaV2"};

                for (String i : cmd) {
                    Runtime.getRuntime().exec(i);
                    Thread.sleep(2000);
                }

                log.info("Se han reiniciado los servicios correctamente!!");
            } catch (IOException | InterruptedException ex) {
                log.error(ex);
            }
            
            //Se limpian todos los arreglos para evitar sobreescritura.
            renombrado = false;
            rezagado = false;
            movido = true;
            bandera = false;
            documentos.clear();
            docNuevos.clear();
            dosificador.clear();
            nombresArchivosTemp.clear();
            nombresArchivosEntrada.clear();
            nombresDocNuevos.clear();
            docRezagados.clear();
            contar();
        }

    }
}
