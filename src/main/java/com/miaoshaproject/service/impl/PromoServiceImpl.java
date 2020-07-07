package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.PromoDoMapper;
import com.miaoshaproject.dataobject.PromoDo;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDoMapper promoDoMapper;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDo promoDo = promoDoMapper.selectByItemId(itemId);
        PromoModel promoModel = convertDoToModel(promoDo);
        if (promoModel == null){
            return null;
        }
        if (promoModel.getStartTime().isAfterNow()){
            promoModel.setStatus(1);
        } else if (promoModel.getEndTime().isBeforeNow()){
            promoModel.setStatus(3);
        } else{
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    private PromoModel convertDoToModel(PromoDo promoDo){
        if(promoDo == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        promoModel.setStartTime(new DateTime(promoDo.getStartTime().getTime()));
        promoModel.setEndTime(new DateTime(promoDo.getEndTime().getTime()));
        BeanUtils.copyProperties(promoDo, promoModel);
        return promoModel;
    }
}
