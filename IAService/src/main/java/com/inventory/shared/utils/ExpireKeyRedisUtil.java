package com.inventory.shared.utils;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.application.usecase.ConfirmPurchaseUseCase;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

@ApplicationScoped
public class ExpireKeyRedisUtil {

    private static final String LINE="|";
    private static final String RESERVATION_KEY="reservation:";
     private static final String RESERVATIONMETA_KEY="reservationmeta:";

   public static String buildKey(String idProduct,String key){
     return RESERVATION_KEY+idProduct+LINE +key;
   } 

   public static String buildMetaKey(String key){
     return key.replace(RESERVATION_KEY, RESERVATIONMETA_KEY);
   }


}


