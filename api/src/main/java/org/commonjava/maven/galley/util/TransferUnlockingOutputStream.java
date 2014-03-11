/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.galley.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.commonjava.maven.galley.model.Transfer;

public class TransferUnlockingOutputStream
    extends FilterOutputStream
{

    private final Transfer transfer;

    public TransferUnlockingOutputStream( final OutputStream out, final Transfer transfer )
    {
        super( out );
        this.transfer = transfer;
    }

    @Override
    public void close()
        throws IOException
    {
        super.close();
        transfer.unlock();
    }

}