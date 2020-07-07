package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.OrderInfoDoMapper;
import com.miaoshaproject.dao.SequenceDoMapper;
import com.miaoshaproject.dataobject.OrderInfoDo;
import com.miaoshaproject.dataobject.SequenceDo;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderInfoDoMapper orderInfoDoMapper;
    @Autowired
    private SequenceDoMapper sequenceDoMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer amount, Integer promoId) throws BusinessException {
        //校验下单状态：商品存在、用户合法、购买数量正确
        ItemModel item = itemService.getItemById(itemId);
        if(item == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品不存在");
        }
        UserModel user = userService.getUserById(userId);
        if(user == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户不存在");
        }
        if(amount <= 0 || amount > item.getStock()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }
        //校验活动信息
        if (promoId != null){
            if (promoId.intValue()!=item.getPromoModel().getId()){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
            } else if (item.getPromoModel().getStatus() != 2){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动未开始");
            }
        }
        //下单成功即减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if(promoId != null){
            orderModel.setItemPrice(item.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(item.getPrice());
        }
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        orderModel.setPromoId(promoId);
        //生成交易流水号
        orderModel.setId(generateOrderNo());
        //销量增加
        itemService.increaseSales(itemId,amount);
        //返回前端
        OrderInfoDo orderInfoDo = convertFromOrderModel(orderModel);
        orderInfoDoMapper.insertSelective(orderInfoDo);
        return orderModel;
    }

    @Transactional(propagation= Propagation.REQUIRES_NEW)
    String generateOrderNo(){
        //16位 = 8位时间+6位自增+2位分库分表
        LocalDateTime now = LocalDateTime.now();
        StringBuilder sb = new StringBuilder();
        String date = now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        sb.append(date);
        SequenceDo sequenceDo = sequenceDoMapper.getSequenceByName("order_info");
        Integer currentValue = sequenceDo.getCurrentValue();
        sequenceDo.setCurrentValue(sequenceDo.getCurrentValue()+sequenceDo.getStep());
        sequenceDoMapper.updateByPrimaryKeySelective(sequenceDo);
        String sequence = String.valueOf(currentValue);
        for (int i = 0; i < 6-sequence.length(); i++){
            sb.append(0);
        }
        sb.append(sequence);
        sb.append("00");
        return sb.toString();
    }

    private OrderInfoDo convertFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderInfoDo orderInfoDo = new OrderInfoDo();
        BeanUtils.copyProperties(orderModel, orderInfoDo);
        return orderInfoDo;
    }
}
