package za.co.taloms.gis.domain.entity;

public enum LayerType {
    VECTOR,
    RASTER,
    TILE;

    public String getDisplayName() {
        return switch (this) {
            case VECTOR -> "Vector";
            case RASTER -> "Raster";
            case TILE -> "Tile";
        };
    }
}