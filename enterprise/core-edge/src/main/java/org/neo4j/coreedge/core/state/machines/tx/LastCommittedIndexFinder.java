/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.core.state.machines.tx;

import org.neo4j.cursor.IOCursor;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.LogicalTransactionStore;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;

import static org.neo4j.coreedge.core.state.machines.tx.LogIndexTxHeaderEncoding.decodeLogIndexFromTxHeader;
import static org.neo4j.kernel.impl.transaction.log.TransactionIdStore.BASE_TX_ID;

/**
 * Finds the last committed transaction in the transaction log, then decodes the header as a raft index.
 * This allows us to correlate raft log with transaction log on recovery.
 */
class LastCommittedIndexFinder
{
    private final TransactionIdStore transactionIdStore;
    private final LogicalTransactionStore transactionStore;
    private final Log log;

    LastCommittedIndexFinder( TransactionIdStore transactionIdStore,
                              LogicalTransactionStore transactionStore, LogProvider logProvider )
    {
        this.transactionIdStore = transactionIdStore;
        this.transactionStore = transactionStore;
        this.log = logProvider.getLog( getClass() );
    }

    long getLastCommittedIndex()
    {
        long lastCommittedIndex;
        long lastTxId = transactionIdStore.getLastCommittedTransactionId();

        if ( lastTxId == BASE_TX_ID )
        {
            lastCommittedIndex = -1;
        }
        else
        {
            byte[] lastHeaderFound = null;
            try ( IOCursor<CommittedTransactionRepresentation> transactions =
                          transactionStore.getTransactions( lastTxId ) )
            {
                while ( transactions.next() )
                {
                    CommittedTransactionRepresentation committedTransactionRepresentation = transactions.get();
                    lastHeaderFound = committedTransactionRepresentation.getStartEntry().getAdditionalHeader();
                }
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
            lastCommittedIndex = decodeLogIndexFromTxHeader( lastHeaderFound );
        }

        log.info( "Last committed index %d", lastCommittedIndex );
        return lastCommittedIndex;
    }
}
