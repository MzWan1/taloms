package za.co.taloms.gis.presentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.security.application.service.AuthorityScopeService;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.dto.VillageResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GisPageControllerTest {

    @Mock private ParcelService parcelService;
    @Mock private TraditionalAuthorityService authorityService;
    @Mock private VillageService villageService;
    @Mock private AuthorityScopeService scopeService;

    @InjectMocks
    private GisPageController controller;

    @Test
    void chiefShouldOnlySeeOwnAuthorityAndVillages() {
        when(scopeService.isCurrentUserChiefOrHeadsman()).thenReturn(true);
        when(scopeService.getCurrentUserAuthorityId()).thenReturn(10L);
        when(authorityService.findById(10L)).thenReturn(TraditionalAuthorityResponse.builder()
                .id(10L).authorityName("Authority A").build());
        when(villageService.findByAuthority(10L)).thenReturn(List.of(
                VillageResponse.builder().id(1L).villageName("Village A").build()));
        when(parcelService.countAll()).thenReturn(5L);

        var model = new ConcurrentModel();
        var view = controller.index(model);

        assertEquals("gis/index", view);
        assertEquals(1, ((List<?>) model.getAttribute("authorities")).size());
        assertEquals(1, ((List<?>) model.getAttribute("villages")).size());
        assertEquals(10L, model.getAttribute("scopedAuthorityId"));
        verify(authorityService).findById(10L);
        verify(villageService).findByAuthority(10L);
        verify(authorityService, never()).findAllActive();
    }

    @Test
    void adminShouldSeeAllAuthoritiesAndVillages() {
        when(scopeService.isCurrentUserChiefOrHeadsman()).thenReturn(false);
        when(scopeService.getCurrentUserAuthorityId()).thenReturn(null);
        when(authorityService.findAllActive()).thenReturn(List.of(
                TraditionalAuthorityResponse.builder().id(10L).authorityName("Authority A").build(),
                TraditionalAuthorityResponse.builder().id(11L).authorityName("Authority B").build()));
        when(villageService.findAll()).thenReturn(List.of(
                VillageResponse.builder().id(1L).villageName("Village A").build(),
                VillageResponse.builder().id(2L).villageName("Village B").build()));
        when(parcelService.countAll()).thenReturn(5L);

        var model = new ConcurrentModel();
        var view = controller.index(model);

        assertEquals("gis/index", view);
        assertEquals(2, ((List<?>) model.getAttribute("authorities")).size());
        assertEquals(2, ((List<?>) model.getAttribute("villages")).size());
        assertNull(model.getAttribute("scopedAuthorityId"));
        verify(authorityService).findAllActive();
        verify(villageService).findAll();
    }
}