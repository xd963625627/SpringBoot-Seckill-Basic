package com.miaoshaproject;

import com.miaoshaproject.dao.PromoDoMapper;
import com.miaoshaproject.dataobject.PromoDo;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.PromoModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Test {
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoDoMapper promoDoMapper;
    @org.junit.jupiter.api.Test
    void test(){
        PromoDo promoDo = promoDoMapper.selectByItemId(5);
        System.out.println(promoDo);
        PromoModel model = promoService.getPromoByItemId(5);
        System.out.println(model);
    }
}
