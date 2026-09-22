package model;

import java.util.Date;

public class Occurrence {
    private Date date;
    private double latitude;
    private double longitude;
    private Species speciesRef;
    // Nem todo eventDate do GBIF traz hora (muitos registros só têm precisão
    // de dia). true por padrão; GBIFApiClient ajusta para false quando o
    // eventDate bruto não trouxer componente de horário.
    private boolean horarioConhecido = true;

    public Occurrence(Date date, double latitude, double longitude, Species speciesRef) {
        setDate(date);
        setCoordinates(latitude, longitude);
        
        if (speciesRef == null) {
            throw new IllegalArgumentException("Toda ocorrência deve estar associada a uma espécie válida.");
        }
        this.speciesRef = speciesRef;
    }

    public void setDate(Date d) {
        if (d != null && d.after(new Date())) {
            throw new IllegalArgumentException("A data da ocorrência não pode ser no futuro.");
        }
        this.date = d;
    }

    public void setCoordinates(double lat, double lon) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude inválida (" + lat + "). Deve estar entre -90 e 90.");
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Longitude inválida (" + lon + "). Deve estar entre -180 e 180.");
        }
        
        this.latitude = lat;
        this.longitude = lon;
    }

    public void setHorarioConhecido(boolean horarioConhecido) {
        this.horarioConhecido = horarioConhecido;
    }

    public Date getDate() { return date; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Species getSpeciesRef() { return speciesRef; }
    public boolean isHorarioConhecido() { return horarioConhecido; }
}