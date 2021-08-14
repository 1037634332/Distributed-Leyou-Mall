import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.pojo.Sku;
import com.leyou.item.service.impl.SpecificationServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpecificationServiceImpl.class)
public class test01 {

    @Autowired
    private SkuMapper skuMapper;
    @Test
    public void test(){
     /*   Sku sku=this.skuMapper.querySpecificationByCategoryId(2600248L);
        System.out.println(sku);*/
    }
}
