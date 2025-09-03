package entity;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrameTree {
    @SerializedName("name")
    private String signature;
    @SerializedName("value")
    private long total;
    private long self;
    private final List<FrameTree> children = new ArrayList<>();
}
