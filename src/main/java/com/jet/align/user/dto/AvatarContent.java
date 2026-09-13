package com.jet.align.user.dto;

/**
 * Bytes de una foto de perfil más su tipo MIME. Es el borde entre la capa web y
 * el servicio: el controller lo arma desde el MultipartFile (un tipo de Spring
 * Web que no debe entrar al dominio) y lo recibe de vuelta para servir la imagen.
 */
public record AvatarContent(

        String contentType,
        byte[] data

) {}
