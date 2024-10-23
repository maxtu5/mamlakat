package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.dao.graph.MonarchNodeRepository;
import com.tuiken.mamlakat.dao.graph.ReignNodeRepository;
import com.tuiken.mamlakat.dao.graph.ThronnyRepository;
import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.Gender;
import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.Reign;
import com.tuiken.mamlakat.model.dtos.Throne;
import com.tuiken.mamlakat.model.dtos.api.CopyToGraphTaskDto;
import com.tuiken.mamlakat.model.graph.MonarchNode;
import com.tuiken.mamlakat.model.graph.ReignNode;
import com.tuiken.mamlakat.model.graph.ThroneNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GraphFirstService {

    private final ThronnyRepository thronnyRepository;
    private final ThroneRoom throneRoom;
    private final MonarchService monarchService;
    private final ProvenanceService provenanceService;
    private final MonarchNodeRepository monarchNodeRepository;
    private final ReignNodeRepository reignNodeRepository;

    //@Transactional
    public void wtf(CopyToGraphTaskDto copyToGraphTaskDto) {
        ThroneNode throneNode = thronnyRepository.findByCountry(copyToGraphTaskDto.getCountry())
                .orElse(null);
        if (throneNode != null)
            thronnyRepository.save(throneNode);
    }
//    @Transactional
    public boolean copyToGraph(CopyToGraphTaskDto copyToGraphTaskDto) throws InterruptedException {
        Throne throne = throneRoom.loadThroneByCountry(copyToGraphTaskDto.getCountry());
        if (throne != null &&
                copyToGraphTaskDto.getFrom() >= 0 &&
                copyToGraphTaskDto.getFrom() < throne.getMonarchsIds().size() &&
                copyToGraphTaskDto.getFrom() <= copyToGraphTaskDto.getTo() &&
                copyToGraphTaskDto.getTo() < throne.getMonarchsIds().size()) {
            // load or create  throne
            ThroneNode throneNode = thronnyRepository.findByCountry(copyToGraphTaskDto.getCountry())
                    .orElse(new ThroneNode(throne.getId().toString(), throne.getName(), throne.getCountry(), new HashSet<>(), null));
            thronnyRepository.save(throneNode);
            // get list of rulers and relatives
            ReignNode lastReignNode = copyToGraphTaskDto.getFrom()==0 ? null : findLastReign(throneNode);
            List<MonarchNode> openMonarchs = new ArrayList<>();
            for (int i = copyToGraphTaskDto.getFrom(); i <= copyToGraphTaskDto.getTo(); i++) {
                Monarch monarch = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(i)));
                System.out.println("== Copy-" + i + " " + monarch.getName());
                lastReignNode = saveMonarchToGraph(monarch, lastReignNode, throneNode, openMonarchs);
                System.out.println("Wait....");
                Thread.sleep(5000);
            }
        }
        return true;
    }

    private ReignNode findLastReign(ThroneNode throneNode) {
        ReignNode retval = reignNodeRepository.findLatestReignForThrone(throneNode.getId());
        ReignNode predecessor = reignNodeRepository.findPredecessor(retval.getId());
        while (predecessor!=null) {
            retval = predecessor;
            predecessor = reignNodeRepository.findPredecessor(retval.getId());
        }
        return retval;
    }

    private ReignNode saveMonarchToGraph(Monarch monarch, ReignNode lastReignNode, ThroneNode throneNode, List<MonarchNode> openMonarchs) {
        MonarchNode monarchNode = createMonarchNode(monarch, openMonarchs);
        Monarch parent = provenanceService.findFather(monarch);
        if (parent != null) {
            MonarchNode fatherNode = createMonarchNode(parent, openMonarchs);
            monarchNode.setFather(fatherNode);
            monarchNodeRepository.save(monarchNode);
        }
        parent = provenanceService.findMother(monarch);
        if (parent != null) {
            MonarchNode motherNode = createMonarchNode(parent, openMonarchs);
            monarchNode.setMother(motherNode);
            monarchNodeRepository.save(monarchNode);
        }
        Set<Monarch> children = provenanceService.findChildren(monarch);
        for (Monarch child : children) {
            MonarchNode childNode = createMonarchNode(child, openMonarchs);
            if (monarch.getGender().equals(Gender.MALE)) {
                childNode.setFather(monarchNode);
            } else {
                childNode.setMother(monarchNode);
            }
            monarchNodeRepository.save(childNode);
        }
        // create Reigns for rulers
        ReignNode oneReignNode=null;
        for (Reign reign : monarch.getReigns()) {
            if (reign.getCountry().equals(throneNode.getCountry())) {
                ReignNode reignNode = createReignNode(reign, throneNode.getCountry(), monarchNode);
                oneReignNode=reignNode;
                reignNodeRepository.save(reignNode);
                throneNode.getRulers().add(reignNode);
                monarchNode.getReigns().add(reignNode);
                monarchNodeRepository.save(monarchNode);
                thronnyRepository.save(throneNode);
            }
        }
        if (lastReignNode==null) {
            throneNode.setLatest(oneReignNode);
            thronnyRepository.save(throneNode);
            return oneReignNode;
        } else {
            lastReignNode.setPredecessor(oneReignNode);
            reignNodeRepository.save(lastReignNode);
            return oneReignNode;
        }
    }

    private ReignNode createReignNode(Reign reign, Country country, MonarchNode monarch) {
        return new ReignNode(UUID.randomUUID().toString(), reign.getTitle(),
                reign.getStart() == null ? null : reign.getStart().atZone(ZoneId.systemDefault()).toLocalDate(),
                reign.getEnd() == null ? null : reign.getEnd().atZone(ZoneId.systemDefault()).toLocalDate(),
                reign.getCoronation()==null?null: reign.getCoronation().atZone(ZoneId.systemDefault()).toLocalDate(),
                country,
                monarch,
                null, null);
    }

    private MonarchNode createMonarchNode(Monarch monarch, List<MonarchNode> soFar) {
        MonarchNode retval = soFar.stream().filter(mn->mn.getId().equals(monarch.getId().toString())).findAny().orElse(null);
        if (retval!=null) {
            return retval;
        }
        retval = monarchNodeRepository.findById(monarch.getId().toString())
                .orElse(null);
        if (retval != null) {
            soFar.add(retval);
            return retval;
        } else {
            retval = new MonarchNode(monarch.getId().toString(), monarch.getUrl(), monarch.getName(), monarch.getGender(),
                    monarch.getBirth() == null ? null : monarch.getBirth().atZone(ZoneId.systemDefault()).toLocalDate(),
                    monarch.getDeath() == null ? null : monarch.getDeath().atZone(ZoneId.systemDefault()).toLocalDate(),
                    monarch.getStatus(), new HashSet<>(), null, null);
            System.out.println("== Created " + retval.getName());
            monarchNodeRepository.save(retval);
            soFar.add(retval);
            return retval;
        }
    }

    @Transactional
    public boolean deleteAllMonarchs() {
        monarchNodeRepository.deleteAll();
        return true;
    }
}
