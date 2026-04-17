package com.btl.n8.Connection;

import com.btl.n8.Model.Bidder;

public interface BidderDAO {
    boolean insert(Bidder bidder);
    boolean updateBalance(Bidder bidder);
    Bidder findById(int id);
}
