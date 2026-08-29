package com.jairomatias.eventix.eligibility.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.RosterImportError;
import com.jairomatias.eventix.eligibility.dto.RosterImportResult;
import com.jairomatias.eventix.eligibility.dto.RosterImportRow;
import com.jairomatias.eventix.eligibility.entity.PromotionMember;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.entity.SchoolRosterImport;
import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolRosterImportRepository;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class SchoolRosterImportService {

    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;

    private final CsvRosterParser csvRosterParser;
    private final NationalIdLookupService nationalIdLookupService;
    private final PromotionMemberRepository promotionMemberRepository;
    private final SchoolPromotionRepository schoolPromotionRepository;
    private final SchoolRosterImportRepository rosterImportRepository;
    private final UserRepository userRepository;

    public SchoolRosterImportService(
            CsvRosterParser csvRosterParser,
            NationalIdLookupService nationalIdLookupService,
            PromotionMemberRepository promotionMemberRepository,
            SchoolPromotionRepository schoolPromotionRepository,
            SchoolRosterImportRepository rosterImportRepository,
            UserRepository userRepository) {
        this.csvRosterParser = csvRosterParser;
        this.nationalIdLookupService = nationalIdLookupService;
        this.promotionMemberRepository = promotionMemberRepository;
        this.schoolPromotionRepository = schoolPromotionRepository;
        this.rosterImportRepository = rosterImportRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RosterImportResult importCsv(
            Long promotionId,
            Long importedById,
            String sourceName,
            byte[] content) {
        validateFile(sourceName, content);

        SchoolPromotion promotion = schoolPromotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("La promoción indicada no existe."));
        User importedBy = userRepository.findById(importedById)
                .orElseThrow(() -> new IllegalArgumentException("El usuario importador no existe."));
        if (importedBy.getRole().getName() != RoleName.ADMINISTRATOR) {
            throw new IllegalArgumentException(
                    "Solo un administrador puede importar padrones escolares.");
        }
        if (!promotion.isActive() || !promotion.getInstitution().isActive()) {
            throw new IllegalArgumentException(
                    "La institución y la promoción deben estar activas para importar el padrón.");
        }

        String checksum = checksum(content);
        if (rosterImportRepository.existsByPromotion_IdAndFileChecksum(promotionId, checksum)) {
            throw new IllegalArgumentException(
                    "Este archivo ya fue importado previamente para la promoción seleccionada.");
        }

        List<RosterImportRow> rows = csvRosterParser.parse(content);
        List<PromotionMember> accepted = new ArrayList<>();
        List<RosterImportError> errors = new ArrayList<>();
        Set<String> seenLookupKeys = new HashSet<>();

        for (RosterImportRow row : rows) {
            try {
                validateRow(row);
                String lookupKey = nationalIdLookupService.lookupKey(row.nationalId());
                if (!seenLookupKeys.add(lookupKey)) {
                    throw new IllegalArgumentException("Cédula duplicada dentro del archivo.");
                }
                if (promotionMemberRepository.existsByPromotion_IdAndNationalIdLookup(
                        promotionId,
                        lookupKey)) {
                    throw new IllegalArgumentException("La persona ya existe en este padrón.");
                }
                accepted.add(new PromotionMember(
                        promotion,
                        row.fullName().trim(),
                        trimToNull(row.studentCode()),
                        lookupKey,
                        nationalIdLookupService.last4(row.nationalId()),
                        trimToNull(row.sourceReference())));
            } catch (IllegalArgumentException exception) {
                errors.add(new RosterImportError(row.rowNumber(), exception.getMessage()));
            }
        }

        promotionMemberRepository.saveAll(accepted);
        SchoolRosterImport rosterImport = rosterImportRepository.save(new SchoolRosterImport(
                promotion,
                sourceName.trim(),
                checksum,
                importedBy,
                rows.size(),
                accepted.size(),
                errors.size()));

        return new RosterImportResult(
                rosterImport.getId(),
                rows.size(),
                accepted.size(),
                errors.size(),
                List.copyOf(errors));
    }

    private void validateFile(String sourceName, byte[] content) {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la fuente del padrón.");
        }
        if (sourceName.trim().length() > 240) {
            throw new IllegalArgumentException("La fuente del padrón excede 240 caracteres.");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El archivo del padrón está vacío.");
        }
        if (content.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("El archivo del padrón no puede superar 5 MB.");
        }
    }

    private void validateRow(RosterImportRow row) {
        if (row.fullName() == null || row.fullName().isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        if (row.fullName().trim().length() > 180) {
            throw new IllegalArgumentException("El nombre completo excede 180 caracteres.");
        }
        if (row.studentCode() != null && row.studentCode().trim().length() > 80) {
            throw new IllegalArgumentException("El código estudiantil excede 80 caracteres.");
        }
        if (row.sourceReference() != null && row.sourceReference().trim().length() > 240) {
            throw new IllegalArgumentException("La referencia de origen excede 240 caracteres.");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String checksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }
}
