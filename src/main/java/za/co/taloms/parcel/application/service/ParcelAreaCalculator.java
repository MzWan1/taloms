package za.co.taloms.parcel.application.service;

import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import java.util.List;

public interface ParcelAreaCalculator {
    Double calculateAreaM2(List<BoundaryPointDto> boundaries);
    Double calculateAreaHectares(List<BoundaryPointDto> boundaries);
    Double[] calculateCentroid(List<BoundaryPointDto> boundaries);
}