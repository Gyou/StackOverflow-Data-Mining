import java.util.ArrayList;
import java.util.List;

public class ComputerDecision {

    List<Double> vector1 = new ArrayList<Double>();
    List<Double> vector2 = new ArrayList<Double>();

    public ComputerDecision(List<Double> string1, List<Double> string2) {

        for (int i=0;i<string1.size();i++) {
            vector1.add(string1.get(i));
        }
        for (int i=0;i<string2.size();i++) {
            vector2.add(string2.get(i));
        }
        //System.out.println(vector1.size());
    }

    // 求余弦相似度
    public double sim() {
        double result = 0;
        result = pointMulti(vector1, vector2) / sqrtMulti(vector1, vector2);

        return result;
    }

    private double sqrtMulti(List<Double> vector1, List<Double> vector2) {
        double result = 0;
        result = squares(vector1) * squares(vector2);
        result = Math.sqrt(result);
        return result;
    }

    // 求平方和
    private double squares(List<Double> vector) {
        double result = 0;
        for (Double integer : vector) {
            result += integer * integer;
        }
        return result;
    }

    // 点乘法
    private double pointMulti(List<Double> vector1, List<Double> vector2) {
        double result = 0;
        for (int i = 0; i < vector1.size(); i++) {
            result += vector1.get(i) * vector2.get(i);
        }
        return result;
    }
}
