package com.bibliotech.stats;

import com.bibliotech.acces.*;
import com.bibliotech.catalogue.document.*;
import com.bibliotech.utilisateur.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final DocumentRepository documentRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final AccesRepository accesRepo;

    @Transactional(readOnly = true)
    public StatsDTO getStats() {
        long totalDocs  = documentRepo.count();
        long totalUsers = utilisateurRepo.count();
        long totalDl    = accesRepo.countByTypeAcces(
                TypeAcces.TELECHARGEMENT);
        long totalLec   = accesRepo.countByTypeAcces(
                TypeAcces.LECTURE_EN_LIGNE);

        List<DocumentStatDTO> top = documentRepo
                .findTop5ByOrderByNombreTelechargementsDesc()
                .stream()
                .map(d -> new DocumentStatDTO(
                        d.getId(), d.getTitre(),
                        d.getAuteur(), d.getNombreTelechargements()))
                .toList();

        List<AccesRecentDTO> recents = accesRepo
                .findTop10ByOrderByDateAccesDesc()
                .stream()
                .map(a -> new AccesRecentDTO(
                        a.getDocument().getTitre(),
                        a.getUtilisateur().getEmail(),
                        a.getTypeAcces().name(),
                        a.getDateAcces().toString()))
                .toList();

        return new StatsDTO(totalDocs, totalUsers,
                totalDl, totalLec, top, recents);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        List<Acces> acces = accesRepo.findAllWithDetails();
        StringBuilder csv = new StringBuilder();
        csv.append("Document,Utilisateur,Type,Date\n");
        for (Acces a : acces) {
            csv.append(String.format("%s,%s,%s,%s\n",
                    a.getDocument().getTitre(),
                    a.getUtilisateur().getEmail(),
                    a.getTypeAcces(),
                    a.getDateAcces()));
        }
        return csv.toString().getBytes();
    }
}