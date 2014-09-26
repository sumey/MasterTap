package com.github.jthuraisamy.mastertap.interfaces;

import com.github.jthuraisamy.mastertap.models.Card;

import java.util.List;
import java.util.Map;

public interface CardDao {
    public void addCard(Card card);
    public void addCvc3(Card card, int unpredictableNumber, String response);
    public List<Card> getCards();
    public Card getCard(String pan);
    public Map<Integer, String> getCvc3MapByCardId(int id);
    public void updateCard(Card card);
    public void deleteCard(Card card);
}
