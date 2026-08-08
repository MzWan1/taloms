package za.co.taloms;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyRequest;
import za.co.taloms.businessoccupancy.application.dto.BusinessOccupancyResponse;
import za.co.taloms.businessoccupancy.application.service.BusinessOccupancyService;
import za.co.taloms.household.application.dto.HouseholdRequest;
import za.co.taloms.household.application.dto.HouseholdResponse;
import za.co.taloms.household.application.service.HouseholdService;
import za.co.taloms.parcel.application.dto.BoundaryPointDto;
import za.co.taloms.parcel.application.dto.ParcelRequest;
import za.co.taloms.parcel.application.dto.ParcelResponse;
import za.co.taloms.parcel.application.service.ParcelService;
import za.co.taloms.pto.application.dto.PTOApprovalRequest;
import za.co.taloms.pto.application.dto.PTORequest;
import za.co.taloms.pto.application.dto.PTOResponse;
import za.co.taloms.pto.application.service.PTOService;
import za.co.taloms.resident.application.dto.ResidentRequest;
import za.co.taloms.resident.application.dto.ResidentResponse;
import za.co.taloms.resident.application.service.ResidentService;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityRequest;
import za.co.taloms.traditionalauthority.application.dto.TraditionalAuthorityResponse;
import za.co.taloms.traditionalauthority.application.dto.VillageRequest;
import za.co.taloms.traditionalauthority.application.dto.VillageResponse;
import za.co.taloms.traditionalauthority.application.service.TraditionalAuthorityService;
import za.co.taloms.traditionalauthority.application.service.VillageService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class E2ETest {

    @Autowired TraditionalAuthorityService authorityService;
    @Autowired VillageService villageService;
    @Autowired ParcelService parcelService;
    @Autowired PTOService ptoService;
    @Autowired HouseholdService householdService;
    @Autowired ResidentService residentService;
    @Autowired BusinessOccupancyService businessService;
    @Autowired za.co.taloms.document.application.service.DocumentService documentService;

    private void uploadPtoDocument(Long ptoId, String docType) {
        try {
            var file = new MockMultipartFile("file", docType + ".pdf", "application/pdf",
                    ("Mock document content for " + docType).getBytes());
            var req = za.co.taloms.document.application.dto.DocumentUploadRequest.builder()
                    .documentType(docType)
                    .entityType("PTO")
                    .entityId(ptoId)
                    .description("Mock " + docType + " document")
                    .notes("Uploaded by E2E test")
                    .build();
            documentService.uploadDocument(file, req, "admin", "127.0.0.1", "E2ETest");
            log.info("Uploaded document: {} for PTO {}", docType, ptoId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document " + docType + " for PTO " + ptoId, e);
        }
    }

    @Test
    void testEndToEndFlow() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("STARTING END-TO-END FLOW TEST");
        log.info("═══════════════════════════════════════════════════════════════");

        // ── 1. Create Traditional Authority ──────────────────────────────
        log.info("--- Step 1: Create Traditional Authority ---");
        var authority = authorityService.create(
                TraditionalAuthorityRequest.builder()
                        .authorityName("Bloemfontein Traditional Council")
                        .chiefName("Chief Mokoena")
                        .headmanName("Headman Dlamini")
                        .contactPhone("+27555123456")
                        .contactEmail("chief@btcouncil.org.za")
                        .physicalAddress("123 Main Street, Bloemfontein")
                        .region("Free State")
                        .build(),
                "admin"
        );
        log.info("Created Traditional Authority: ID={}, Name={}", authority.getId(), authority.getAuthorityName());

        // ── 2. Create Village ────────────────────────────────────────────
        log.info("--- Step 2: Create Village ---");
        var village = villageService.create(
                VillageRequest.builder()
                        .villageName("Botanical Gardens Village")
                        .region("Free State")
                        .headmanName("Headman Dlamini")
                        .description("Near the botanical gardens")
                        .traditionalAuthorityId(authority.getId())
                        .build()
        );
        log.info("Created Village: ID={}, Name={}", village.getId(), village.getVillageName());

        // ── 3. Create 2 Parcels (one for household, one for business) ─────
        log.info("--- Step 3: Create 2 Parcels ---");

        var boundaries = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-28.0).longitude(26.0).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-28.0).longitude(26.1).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-27.9).longitude(26.1).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-27.9).longitude(26.0).build()
        );

        var boundaries2 = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-28.1).longitude(26.2).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-28.1).longitude(26.3).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-28.0).longitude(26.3).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-28.0).longitude(26.2).build()
        );

        var boundaries3 = List.of(
                BoundaryPointDto.builder().sequence(1).latitude(-28.2).longitude(26.4).build(),
                BoundaryPointDto.builder().sequence(2).latitude(-28.2).longitude(26.5).build(),
                BoundaryPointDto.builder().sequence(3).latitude(-28.1).longitude(26.5).build(),
                BoundaryPointDto.builder().sequence(4).latitude(-28.1).longitude(26.4).build()
        );

        var parcel1 = parcelService.createParcel(
                ParcelRequest.builder()
                        .standNumber("STAND-001")
                        .villageId(village.getId())
                        .boundaries(boundaries)
                        .notes("Household parcel")
                        .build(),
                "admin"
        );
        log.info("Created Parcel 1: ID={}, Parcel#={}, Stand={}",
                parcel1.getId(), parcel1.getParcelNumber(), parcel1.getStandNumber());

        var parcel2 = parcelService.createParcel(
                ParcelRequest.builder()
                        .standNumber("STAND-002")
                        .villageId(village.getId())
                        .boundaries(boundaries2)
                        .notes("Business parcel")
                        .build(),
                "admin"
        );
        log.info("Created Parcel 2: ID={}, Parcel#={}, Stand={}",
                parcel2.getId(), parcel2.getParcelNumber(), parcel2.getStandNumber());

        // ── 4. Create PTO for Household Parcel ───────────────────────────
        log.info("--- Step 4: Create PTO for Household Parcel ---");
        var pto1Request = PTORequest.builder()
                .ptoHolderName("Thabo Mokoena")
                .idNumber("8001015009017")
                .contactPhone("+27712345678")
                .contactEmail("thabo.mokoena@example.com")
                .purpose("RESIDENTIAL")
                .issueDate(LocalDate.now())
                .villageId(village.getId())
                .traditionalAuthorityId(authority.getId())
                .parcelId(parcel1.getId())
                .standNumber("STAND-001")
                .parcelNumber(parcel1.getParcelNumber())
                .standArea(1500.0)
                .surveyReference("SURV-2024-001")
                .build();

        var pto1Pending = ptoService.createPTO(pto1Request, "admin");
        log.info("Created PTO 1 (PENDING): ID={}, Number={}, Status={}",
                pto1Pending.getId(), pto1Pending.getPtoNumber(), pto1Pending.getStatus());

        // Upload required documents for PTO 1 approval
        log.info("--- Step 5a: Upload required documents for PTO 1 ---");
        uploadPtoDocument(pto1Pending.getId(), "ID_COPY");
        uploadPtoDocument(pto1Pending.getId(), "TA_ALLOCATION_LETTER");
        uploadPtoDocument(pto1Pending.getId(), "SITE_SKETCH");

        // ── 5. Approve PTO 1 (parcel becomes ALLOCATED) ───────────────────
        log.info("--- Step 5: Approve PTO 1 ---");
        var pto1Approved = ptoService.approvePTO(
                pto1Pending.getId(),
                PTOApprovalRequest.builder()
                        .notes("Approved for residential use")
                        .ipAddress("127.0.0.1")
                        .userAgent("E2ETest")
                        .build(),
                "admin"
        );
        log.info("Approved PTO 1: ID={}, Status={}, Parcel Status={}",
                pto1Approved.getId(), pto1Approved.getStatus(),
                parcelService.findById(parcel1.getId()).getStatus());

        // ── 6. Create PTO for Business Parcel ─────────────────────────────
        log.info("--- Step 6: Create PTO for Business Parcel ---");
        var pto2Request = PTORequest.builder()
                .ptoHolderName("Sipho Dlamini")
                .idNumber("7505153012018")
                .contactPhone("+27823456789")
                .contactEmail("sipho.dlamini@example.com")
                .purpose("BUSINESS")
                .issueDate(LocalDate.now())
                .villageId(village.getId())
                .traditionalAuthorityId(authority.getId())
                .parcelId(parcel2.getId())
                .standNumber("STAND-002")
                .parcelNumber(parcel2.getParcelNumber())
                .standArea(2000.0)
                .surveyReference("SURV-2024-002")
                .build();

        var pto2Pending = ptoService.createPTO(pto2Request, "admin");
        log.info("Created PTO 2 (PENDING): ID={}, Number={}, Status={}",
                pto2Pending.getId(), pto2Pending.getPtoNumber(), pto2Pending.getStatus());

        // Upload required documents for PTO 2 approval (BUSINESS requires COMMUNITY_RESOLUTION)
        log.info("--- Step 7a: Upload required documents for PTO 2 ---");
        uploadPtoDocument(pto2Pending.getId(), "ID_COPY");
        uploadPtoDocument(pto2Pending.getId(), "TA_ALLOCATION_LETTER");
        uploadPtoDocument(pto2Pending.getId(), "SITE_SKETCH");
        uploadPtoDocument(pto2Pending.getId(), "COMMUNITY_RESOLUTION");

        // ── 7. Approve PTO 2 (parcel becomes ALLOCATED) ───────────────────
        log.info("--- Step 7: Approve PTO 2 ---");
        var pto2Approved = ptoService.approvePTO(
                pto2Pending.getId(),
                PTOApprovalRequest.builder()
                        .notes("Approved for business use")
                        .ipAddress("127.0.0.1")
                        .userAgent("E2ETest")
                        .build(),
                "admin"
        );
        log.info("Approved PTO 2: ID={}, Status={}, Parcel Status={}",
                pto2Approved.getId(), pto2Approved.getStatus(),
                parcelService.findById(parcel2.getId()).getStatus());

        // ── 8. Household auto-created from PTO 1 approval ─────────────────
        log.info("--- Step 8: Verify auto-created Household (linked to Parcel 1 + PTO 1) ---");
        var household = householdService.findActiveByParcelId(parcel1.getId());
        log.info("Auto-created Household: ID={}, Head={}, Parcel ID={}, PTO ID={}",
                household.getId(), household.getHouseholdHeadName(),
                household.getParcelId(), household.getPtoId());

        // ── 9. Create 2 Residents (members of the household) ──────────────
        log.info("--- Step 9: Create 2 Residents (household members) ---");

        var resident1 = residentService.createResident(
                ResidentRequest.builder()
                        .fullName("Thabo Mokoena")
                        .idNumber("8001015009017")
                        .dateOfBirth(LocalDate.of(1980, 1, 1))
                        .gender("MALE")
                        .relationshipType("HOUSEHOLD_HEAD")
                        .occupation("Engineer")
                        .contactPhone("+27712345678")
                        .contactEmail("thabo.mokoena@example.com")
                        .householdId(household.getId())
                        .notes("Household head")
                        .build(),
                "admin"
        );
        log.info("Created Resident 1: ID={}, Name={}, Household ID={}",
                resident1.getId(), resident1.getFullName(), resident1.getHouseholdId());

        var resident2 = residentService.createResident(
                ResidentRequest.builder()
                        .fullName("Sarah Mokoena")
                        .idNumber("8503221056012")
                        .dateOfBirth(LocalDate.of(1985, 3, 22))
                        .gender("FEMALE")
                        .relationshipType("SPOUSE")
                        .occupation("Teacher")
                        .contactPhone("+27723456789")
                        .contactEmail("sarah.mokoena@example.com")
                        .householdId(household.getId())
                        .notes("Spouse of household head")
                        .build(),
                "admin"
        );
        log.info("Created Resident 2: ID={}, Name={}, Household ID={}",
                resident2.getId(), resident2.getFullName(), resident2.getHouseholdId());

        // ── 10. Business auto-created from BUSINESS PTO 2 approval ─────────
        log.info("--- Step 10: Verify auto-created Business (linked to Parcel 2 + PTO 2) ---");
        var businessList = businessService.findByParcelId(parcel2.getId());
        var business = businessList.stream()
                .filter(b -> b.getPtoId() != null && b.getPtoId().equals(pto2Approved.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Auto-created business not found for PTO 2"));
        log.info("Auto-created Business: ID={}, Name={}, Parcel ID={}, PTO ID={}",
                business.getId(), business.getBusinessName(),
                business.getParcelId(), business.getPtoId());

        // ── 11. Create Household without PTO (testing the new flexibility) ──
        log.info("--- Step 11: Create Household WITHOUT PTO (testing optional PTO) ---");

        // Need a third parcel for this
        var parcel3 = parcelService.createParcel(
                ParcelRequest.builder()
                        .standNumber("STAND-003")
                        .villageId(village.getId())
                        .boundaries(boundaries3)
                        .notes("Household parcel without PTO")
                        .build(),
                "admin"
        );

        var householdNoPto = householdService.createHousehold(
                HouseholdRequest.builder()
                        .householdHeadName("Lebo Nkosi")
                        .householdHeadIdNumber("7802023014015")
                        .contactPhone("+27634567890")
                        .contactEmail("lebo.nkosi@example.com")
                        .parcelId(parcel3.getId())
                        .registrationDate(LocalDate.now())
                        .notes("Household created without PTO (optional)")
                        .build(),
                "admin"
        );
        log.info("Created Household (no PTO): ID={}, Head={}, Parcel ID={}, PTO ID={}",
                householdNoPto.getId(), householdNoPto.getHouseholdHeadName(),
                householdNoPto.getParcelId(), householdNoPto.getPtoId());

        // ── 12. Verify everything ─────────────────────────────────────────
        log.info("--- Step 12: Verification ---");

        var households = householdService.findAll();
        log.info("Total households: {}", households.size());

        var businesses = businessService.findAll();
        log.info("Total businesses: {}", businesses.size());

        var residents = residentService.findAll();
        log.info("Total residents: {}", residents.size());

        var ptos = ptoService.findAll();
        log.info("Total PTOs: {}", ptos.size());

        var activePtos = ptoService.findByStatus(za.co.taloms.pto.domain.entity.PTOStatus.ACTIVE);
        log.info("Active PTOs: {}", activePtos.size());

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("SUMMARY");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Traditional Authority: {} (ID={})", authority.getAuthorityName(), authority.getId());
        log.info("Village: {} (ID={})", village.getVillageName(), village.getId());
        log.info("Parcel 1: {} (ID={}, Status={})", parcel1.getParcelNumber(), parcel1.getId(), parcel1.getStatus());
        log.info("Parcel 2: {} (ID={}, Status={})", parcel2.getParcelNumber(), parcel2.getId(), parcel2.getStatus());
        log.info("PTO 1: {} (ID={}, Status={}, Holder={})", pto1Approved.getPtoNumber(), pto1Approved.getId(), pto1Approved.getStatus(), pto1Approved.getPtoHolderName());
        log.info("PTO 2: {} (ID={}, Status={}, Holder={})", pto2Approved.getPtoNumber(), pto2Approved.getId(), pto2Approved.getStatus(), pto2Approved.getPtoHolderName());
        log.info("Household 1: Head={}, Parcel ID={}, PTO ID={}", household.getHouseholdHeadName(), household.getParcelId(), household.getPtoId());
        log.info("Household 2 (no PTO): Head={}, Parcel ID={}, PTO ID={}", householdNoPto.getHouseholdHeadName(), householdNoPto.getParcelId(), householdNoPto.getPtoId());
        log.info("Resident 1: {} (ID={}, Household ID={})", resident1.getFullName(), resident1.getId(), resident1.getHouseholdId());
        log.info("Resident 2: {} (ID={}, Household ID={})", resident2.getFullName(), resident2.getId(), resident2.getHouseholdId());
        log.info("Business: {} (ID={}, Parcel ID={}, PTO ID={})", business.getBusinessName(), business.getId(), business.getParcelId(), business.getPtoId());
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("END-TO-END FLOW TEST COMPLETE - ALL ENTITIES CREATED SUCCESSFULLY!");
        log.info("═══════════════════════════════════════════════════════════════");
    }
}
