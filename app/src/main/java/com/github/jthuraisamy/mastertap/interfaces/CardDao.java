package com.github.jthuraisamy.mastertap.interfaces;

import com.github.jthuraisamy.mastertap.models.Card;

import java.util.List;
import java.util.Map;

public interface CardDao {
    void addCard(Card card);
    void addCvc3(Card card, int unpredictableNumber, String response);
    List<Card> getCards();
    Card getCard(String pan);
    Map<Integer, String> getCvc3MapByCardId(int id);
    void updateCard(Card card);
    void deleteCard(Card card);
}
