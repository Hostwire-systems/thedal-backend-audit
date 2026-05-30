package com.thedal.thedal_app.voter;

import java.util.List;

public interface VoterMongoRepositoryCustom {
    void saveVoterMongoWithNullFields(VoterMongo voterMongo);
    void bulkUpsertVoterMongoWithDeduplication(List<VoterMongo> voterMongoList);
}
