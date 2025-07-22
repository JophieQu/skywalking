package entity;

import com.google.perftools.profiles.ProfileProto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
class RawFrameTree {
    private long locationId;
    private long total;
    private long self;
    private final Map<Long, RawFrameTree> children = new HashMap<>();
}

public class FrameTreeBuilder {
    private final ProfileProto.Profile profile;
    private final RawFrameTree root;

    public FrameTreeBuilder(ProfileProto.Profile profile) {
        this.profile = profile;
        this.root = new RawFrameTree(0, 0, 0);
    }

    private FrameTree parseTree(RawFrameTree rawTree) {
        FrameTree tree = new FrameTree(getSignature(rawTree.getLocationId()), rawTree.getTotal(), rawTree.getSelf());
        for (RawFrameTree rawChild : rawTree.getChildren().values()) {
            FrameTree child = parseTree(rawChild);
            tree.getChildren().add(child);
        }
        return tree;
    }

    private String getSignature(long locationId) {
        if (locationId == 0) {
            return "root";
        }
        ProfileProto.Location location = profile.getLocation((int) locationId - 1);
        return location.getLineList().stream().map((line) -> {
            ProfileProto.Function function = profile.getFunction((int) line.getFunctionId() - 1);
            String functionName = profile.getStringTable((int) function.getName());
            return functionName + ":" + line.getLine();
        }).collect(Collectors.joining(";"));
    }

    public FrameTree build() {
        for (ProfileProto.Sample sample : profile.getSampleList()) {
            mergeSample(sample);
        }
        return parseTree(this.root);
    }

    private void mergeSample(ProfileProto.Sample sample) {
        // 合并Sample数据
        Map<Long, RawFrameTree> children = root.getChildren();
        List<Long> locationIdList = new ArrayList<>(sample.getLocationIdList());
        Collections.reverse(locationIdList);
        int size = locationIdList.size();
        for (int i = 0; i < size; i++) {
            boolean isEnd = i == size - 1;
            long locationId = locationIdList.get(i);
            if (children.containsKey(locationId)) {
                RawFrameTree child = children.get(locationId);
                child.setTotal(child.getTotal() + 1);
                child.setSelf(child.getSelf() + (isEnd ? 1 : 0));
                children = child.getChildren();
            } else {
                RawFrameTree child = new RawFrameTree(locationId, 1, (isEnd ? 1 : 0));
                children.put(locationId, child);
                children = child.getChildren();
            }
        }
        root.setTotal(root.getTotal() + 1);
    }
} 