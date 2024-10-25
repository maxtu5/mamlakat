package com.tuiken.mamlakat.model;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public enum House {

    TUDOR("Tudor"),
    WINDSOR("Windsor"),
    SAXE_COBURG_AND_GOTHA("Saxe-Coburg and Gotha"),
    HANOVER("Hanover"),
    STUART("Stuart"),
    ORANGE_NASSAU("Orange-Nassau"),
    GREY("Grey"),
    YORK("York"),
    LANCASTER("Lancaster"),
    PLANTAGENET("Plantagenet"),
    ANGEVIN("Angevin"),
    BLOIS("Blois"),
    NORMANDY("Normandy"),
    GODWIN("Godwin"),
    WESSEX("Wessex"),
    KNYTLINGA("Knýtlinga"),
    SAXE_COBURG_SAALFELD("Saxe-Coburg-Saalfeld"),
    GLUCKSBURG("Glücksburg"),
    BERNADOTTE("Bernadotte"),
    HOLSTEIN_GOTTORP("Holstein-Gottorp"),
    OLDENBURG("Oldenburg"),
    BONDE("Bonde"),
    PALATINATE_NEUMARKT("Palatinate-Neumarkt"),
    GRIFFIN("Griffin"),
    ESTRIDSEN("Estridsen"),
    BJELBO("Bjelbo"), //
    SVERRE("Sverre"),
    GILLE("Gille"),
    HARDRADA("Hardrada"),
    FAIRHAIR("Fairhair"),
    ST_OLAF("St. Olaf"),
    BELGUIM("Belgium"),
    HESSE_KASSEL("Hesse-Kassel"),
    PALATINATE_ZWEIBRUCKEN("Palatinate-Zweibrücken"),
    VASA("Vasa"),
    MECKLENBURG_SCHWERIN("Mecklenburg-Schwerin"),
    ERIC("Eric"),
    SVERKER("Sverker"),
    STENKIL("Stenkil"),
    BOURBON("Bourbon"),
    SAVOY("Savoy"),
    HABSBURG("Habsburg"),
    HABSBURG_LORRAINE("Habsburg-Lorraine"),
    LORRAINE("Lorraine"),
    WITTELSBACH("Wittelsbach"),
    LUXEMBOURG("Luxembourg"),
    HOCHENSTAUFEN("Hohenstaufen"),
    WELF("Welf"),
    SUPPLINBURG("Supplinburg"),
    SALIAN("Salian"),
    OTTONIAN("Ottonian"),
    AMSBERG("Amsberg"),
    RUFFO_DI_CALABRIA("Ruffo di Calabria"),
    TECK("Teck"),
    PALATINATE_SIMMERN("Palatinate-Simmern"),
    NEVILLE("Neville"),
    CAPET("Capet"),
    TAILLEFER("Taillefer"),
    RAMNULFIDS("Ramnulfids"),
    FLANDERS("Flanders"),
    RURIK("Rurik"),
    HOHENZOLLERN_SIGMARINGEN("Hohenzollern-Sigmaringen"),
    REUSS("Reuss"),
    ORLEANS("Orléans"),
    BRAGANZA("Braganza"),
    AVIZ("Aviz"),
    SAXE_GOTHA_ALTENBURG("Saxe-Gotha-Altenburg"),
    SEYMOUR("Seymour"),
    IVREA("Ivrea"),
    MECKLENBURG_STRELITZ("Mecklenburg-Strelitz"),
    JELLING("Jelling"),
    GORM("Gorm"),
    BARCELONA("Barcelona"),
    WETTIN("Wettin"),
    VALOIS("Valois"),
    FARNESE("Farnese"),
    BURGUNDY("Burgundy"),
    TRASTAMARA("Trastámara"),
    BONAPARTE("Bonaparte"),
    MEDICI("Medici"),
    PORTUGUESE_BURGUNDY("Portuguese Burgundy"),
    HOHENZOLLERN("Hohenzollern"),
    AVIS("Avis"),
    MENESES("Meneses");

    private final String label;

    House(String label) {
        this.label = label;
    }

    public static House HouseFromBeginningOfString(String src) {
        for (House h : House.values()) {
            if (src.toUpperCase().startsWith(h.label.toUpperCase())) {
                return h;
            }
        }
        if (!src.toUpperCase().contains("HOUSE") && !src.toUpperCase().contains("AGNATIC")) {
            log.info("!!!!! ==== House detection ===== !!!!!!\nUnrecognized: " + src);
        }
        return null;
    }

}
