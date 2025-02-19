/*
 * Copyright (c) 2002-2025, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.qrcode.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.paris.lutece.plugins.qrcode.service.LogoQrCode;
import fr.paris.lutece.plugins.qrcode.service.QrCodeBuilder;
import fr.paris.lutece.plugins.qrcode.business.QrCodeConfig;
import fr.paris.lutece.plugins.qrcode.business.QrCodeConfigHome;
import fr.paris.lutece.portal.service.file.FileServiceException;
import fr.paris.lutece.portal.service.file.IFileStoreServiceProvider;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;

public class QrCodeDisplay extends HttpServlet
{
    private static final long serialVersionUID = 9005457149901889275L;

    private static final String MARK_QRCODE_CONFIG = "qrconfig";
    private static final String MARK_MESSAGE = "msg";

    private static final String IMG_BASE64_OPENER = "<img src='data:image/png;base64,";
    private static final String IMG_BASE64_CLOSER = "' />";

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     */
    public void processRequest( HttpServletRequest request, HttpServletResponse response )
    {
        int nIdQrCodeConfig = request.getParameter( MARK_QRCODE_CONFIG ) == null ? -1 : Integer.parseInt( request.getParameter( MARK_QRCODE_CONFIG ) );

        QrCodeConfig qrCodeConfig = null;
        try
        {
            qrCodeConfig = QrCodeConfigHome.findByPrimaryKey( nIdQrCodeConfig ).orElse( QrCodeConfig.getDefaultQrCodeConfig( ) );
        }
        catch( NumberFormatException e )
        {
            AppLogService.error( "Error parsing the id of the QrCodeConfig", e );
            throw new AppException( "Error parsing the id of the QrCodeConfig", e );
        }

        String _strMessage;
        if ( qrCodeConfig.getConfigurationType( ) == QrCodeConfig.ConfigurationType.URL.getValue( ) )
        {
            _strMessage = request.getParameter( MARK_MESSAGE );
        }
        else
        {
            StringBuilder sb = new StringBuilder( );
            for ( String param : request.getParameterMap( ).keySet( ) )
            {
                if ( param.equals( MARK_QRCODE_CONFIG ) )
                {
                    continue;
                }
                sb.append( param );
                sb.append( "=" );
                sb.append( request.getParameter( param ) );
                sb.append( "&" );
            }
            _strMessage = sb.toString( );
        }

        QrCodeBuilder qrCodeBuilder = new QrCodeBuilder( _strMessage );
        BufferedImage img = null;

        if ( qrCodeConfig.getImageLogo( ) != null )
        {
            IFileStoreServiceProvider fileStoreService = QrCodeConfigHome.getFileStoreServiceProvider( );
            try
            {
                qrCodeBuilder.addLogo( new LogoQrCode( 0.2, fileStoreService.getInputStream( qrCodeConfig.getImageLogo( ).getFileKey( ) ) ) );
            }
            catch( FileServiceException e )
            {
                AppLogService.error( "Error reading logo file", e );
                throw new AppException( "Error reading logo file", e );
            }
        }

        try
        {
            img = qrCodeBuilder.build( ).toImage( );
        }
        catch( Exception e )
        {
            AppLogService.error( "Error generating QR code image", e );
            throw new AppException( "Error generating QR code image", e );
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream( );
        try
        {
            ImageIO.write( img, "jpg", baos );
        }
        catch( IOException e )
        {
            AppLogService.error( "Error writing QR code to byte array" );
            throw new AppException( "Error writing QR code to byte array" );
        }

        StringBuilder sb = new StringBuilder( );
        sb.append( IMG_BASE64_OPENER );
        sb.append( Base64.getEncoder( ).encodeToString( baos.toByteArray( ) ) );
        sb.append( IMG_BASE64_CLOSER );

        response.setContentType( "text/html" );
        try
        {
            response.getWriter( ).write( sb.toString( ) );
        }
        catch( IOException e )
        {
            AppLogService.error( "Error writing QR code to response" );
            throw new AppException( "Error writing QR code to response" );
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     * @throws ServletException
     *             the servlet Exception
     * @throws IOException
     *             the io exception
     */
    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    {
        processRequest( request, response );
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     * @throws ServletException
     *             the servlet Exception
     * @throws IOException
     *             the io exception
     */
    @Override
    protected void doPost( HttpServletRequest request, HttpServletResponse response )
    {
        processRequest( request, response );
    }

    /**
     * Returns a short description of the servlet.
     * 
     * @return message
     */
    @Override
    public String getServletInfo( )
    {
        return "Servlet serving Qr Code images";
    }
}
