import lombok.Getter;
public class TestLombok {
    @Getter private String name = "test";
    public static void main(String[] args) {
        var t = new TestLombok();
        System.out.println(t.getName());
    }
}