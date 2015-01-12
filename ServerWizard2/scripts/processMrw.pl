#! /usr/bin/perl
# IBM_PROLOG_BEGIN_TAG
# This is an automatically generated prolog.
#
# $Source: src/usr/targeting/common/processMrw.pl $
#
# OpenPOWER HostBoot Project
#
# Contributors Listed Below - COPYRIGHT 2015
# [+] International Business Machines Corp.
#
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied. See the License for the specific language governing
# permissions and limitations under the License.
#
# IBM_PROLOG_END_TAG

use strict;
use XML::Simple;
use Data::Dumper;
use Targets;
use Math::BigInt;
use Getopt::Long;
use File::Basename;

my $VERSION = "1.0.0";

my $force          = 0;
my $serverwiz_file = "";
my $version        = 0;
my $debug          = 0;

GetOptions(
    "f"   => \$force,             # numeric
    "x=s" => \$serverwiz_file,    # string
    "d"   => \$debug,
    "v"   => \$version
  )                               # flag
  or printUsage();

if ( $version == 1 )
{
    die "\nprocessMrw.pl\tversion $VERSION\n";
}

if ( $serverwiz_file eq "" )
{
    printUsage();
}

$XML::Simple::PREFERRED_PARSER = 'XML::Parser';

my $targets = Targets->new;
if ( $force == 1 ) { $targets->{force} = 1; }
if ( $debug == 1 ) { $targets->{debug} = 1; }
$targets->setVersion($VERSION);
my $xmldir = dirname($serverwiz_file);
$targets->loadXML($serverwiz_file);

#--------------------------------------------------
## loop through all targets and do stuff
foreach my $target ( sort keys %{ $targets->getAllTargets() } )
{
    processSystem( $targets, $target );
    processProcessor( $targets, $target );
    processMembuf( $targets, $target );
}

## check for errors
foreach my $target ( keys %{ $targets->getAllTargets() } )
{
    errorCheck( $targets, $target );
}

#--------------------------------------------------
## write out final XML
my $xml_fh;
my $filename =
    $xmldir . "/"
  . $targets->getAttribute( "/sys-0", "SYSTEM_NAME" )
  . "_hb.mrw.xml";
print "Creating XML: $filename\n";
open( $xml_fh, ">$filename" );
$targets->printXML( $xml_fh, "top" );
close $xml_fh;
if ( !$targets->{errorsExist} )
{
    print "MRW created successfully!\n";
}

#--------------------------------------------------
#--------------------------------------------------
## Processing subroutines

#--------------------------------------------------
## ERROR checking
sub errorCheck
{
    my $targets = shift;
    my $target  = shift;
    my $type    = $targets->getType($target);

    ## error checking even for connections are done with attribute checks
    ##  since connections simply create attributes at source and/or destination
    ##
    ## also error checking after processing is complete vs during
    ## processing is easier
    my %attribute_checks = (
        SYS         => ['SYSTEM_NAME'],
        PROC_MASTER => [],
        PROC        => ['FSI_MASTER_CHIP'],
        MEMBUF => [ 'PHYS_PATH', 'EI_BUS_TX_MSBSWAP', 'FSI_MASTER_PORT|0xFF' ],
        DIMM   => ['EEPROM_VPD_PRIMARY_INFO/devAddr'],
    );
    my %error_msg = (
        'EEPROM_VPD_PRIMARY_INFO/devAddr' =>
          'I2C connection to target is not defined',
        'FSI_MASTER_PORT' => 'This target is missing a required FSI connection',
        'FSI_MASTER_CHIP' => 'This target is missing a required FSI connection',
        'EI_BUS_TX_MSBSWAP' =>
          'DMI connection is missing to this membuf from processor',
        'PHYS_PATH' =>
          'DMI connection is missing to this membuf from processor',
    );

    my @errors;
    if ( $targets->getMasterProc() eq $target )
    {
        $type = "MASTER_PROC";
    }
    foreach my $attr ( @{ $attribute_checks{$type} } )
    {
        my ( $a,         $v )     = split( /\|/, $attr );
        my ( $a_complex, $field ) = split( /\//, $a );
        if ( $field ne "" )
        {
            if (
                $targets->isBadComplexAttribute(
                    $target, $a_complex, $field, $v
                )
              )
            {
                push(
                    @errors,
                    sprintf(
                        "$a attribute is invalid (Target=%s)\n\t%s\n",
                        $target, $error_msg{$a}
                    )
                );
            }
        }
        else
        {
            if ( $targets->isBadAttribute( $target, $a, $v ) )
            {
                push(
                    @errors,
                    sprintf(
                        "$a attribute is invalid (Target=%s)\n\t%s\n",
                        $target, $error_msg{$a}
                    )
                );
            }
        }
    }
    if ( $type eq "PROC" )
    {
        ## note: DMI is checked on membuf side so don't need to check that here
        ## this checks if at least 1 abus is connected
        my $found_abus = 0;
        my $abus_error = "";
        foreach my $child ( @{ $targets->getTargetChildren($target) } )
        {
            my $child_type = $targets->getBusType($child);
            if ( $child_type eq "ABUS" || $child_type eq "XBUS" )
            {
                if ( $targets->getMasterProc() ne $target )
                {
                    if ( !$targets->isBadAttribute( $child, "PEER_TARGET" ) )
                    {
                        $found_abus = 1;
                    }
                    else
                    {
                        $abus_error =
                          sprintf(
"proc not connected to proc via Abus or Xbus (Target=%s)",
                            $child );
                    }
                }
            }
        }
        if ($found_abus)
        {
            $abus_error = "";
        }
        else
        {
            push( @errors, $abus_error );
        }
    }
    if ( $errors[0] )
    {
        foreach my $err (@errors)
        {
            print "ERROR: $err\n";
        }
        $targets->myExit(3);
    }
}

#--------------------------------------------------
## System
##

sub processSystem
{
    my $targets = shift;
    my $target  = shift;
    my $type    = $targets->getType($target);
    if ( $type ne "SYS" )
    {
        return;
    }
    $targets->setAttribute( $target, "MAX_MCS_PER_SYSTEM",
        $targets->{NUM_PROCS_PER_NODE} * $targets->{MAX_MCS} );
    $targets->setAttribute( $target, "MAX_PROC_CHIPS_PER_NODE",
        $targets->{NUM_PROCS_PER_NODE} );
}

#--------------------------------------------------
## Processor
##

sub processProcessor
{
    my $targets = shift;
    my $target  = shift;
    my $type    = $targets->getType($target);
    if ( $type ne "PROC" )
    {
        return;
    }

    #########################
    ## Copy PCIE attributes from socket
    ## In serverwiz, processor instances are not unique
    ## because plugged into socket
    ## so processor instance unique attribute are socket level
    my $socket_target =
      $targets->getTargetParent( $targets->getTargetParent($target) );
    foreach my $attr (
        sort (
            keys %{ $targets->getTarget($socket_target)->{TARGET}->{attribute} }
        ) )
    {
        if (   $attr eq "LOCATION_CODE"
            || $attr =~ /PROC\_PCIE/
            || $attr eq "FRU_ID" )
        {
            $targets->setAttribute( $target, $attr,
                $targets->getAttribute( $socket_target, $attr ) );
        }
    }
    $targets->log( $target, "Processing PROC" );
    foreach my $child ( @{ $targets->getTargetChildren($target) } )
    {
        $targets->log( $target, "Processing PROC child: $child" );
        processAbus( $targets, $child );
        processFsi( $targets, $child, $target );
        processPcie( $targets, $child, $target );
        processMcs( $targets, $child, $target );
    }

    ## create mvpd's and sbe's
    my $path  = $targets->getAttribute( $target, "PHYS_PATH" );
    my $model = $targets->getAttribute( $target, "MODEL" );
    if ( $model eq "VENICE" )
    {
        setEepromAttributes( $targets, "EEPROM_VPD_PRIMARY_INFO", $target,
            $path, "0", "0xA0", "0", "0x02", "0x40", "0x80", "0x05" );
        setEepromAttributes( $targets, "EEPROM_VPD_BACKUP_INFO", $target, $path,
            "1", "0xA0", "0", "0x02", "0x40", "0x80", "0x05" );
        setEepromAttributes( $targets, "EEPROM_SBE_PRIMARY_INFO", $target,
            $path, "0", "0xA2", "0", "0x02", "0x40", "0x80", "0x05" );
        setEepromAttributes( $targets, "EEPROM_SBE_BACKUP_INFO", $target, $path,
            "1", "0xA2", "0", "0x02", "0x40", "0x80", "0x05" );
        setEepromAttributes( $targets, "EEPROM_VPD_FRU_INFO", $target, $path,
            "0", "0xA0", "0", "0x02", "0x40", "0x80", "0x05", "0++" );
    }
    ## initialize master processor FSI's
    $targets->setAttributeField( $target, "FSI_OPTION_FLAGS", "flipPort", "1" );

    if ( $target eq $targets->getMasterProc() )
    {
        $targets->setAttributeField( $target, "FSI_OPTION_FLAGS", "reserved",
            "0" );
        $targets->setAttribute( $target, "FSI_MASTER_CHIP",    "physical:sys" );
        $targets->setAttribute( $target, "FSI_MASTER_PORT",    "0xFF" );
        $targets->setAttribute( $target, "ALTFSI_MASTER_CHIP", "physical:sys" );
        $targets->setAttribute( $target, "ALTFSI_MASTER_PORT", "0xFF" );
        $targets->setAttribute( $target, "FSI_MASTER_TYPE",    "NO_MASTER" );
        $targets->setAttribute( $target, "FSI_SLAVE_CASCADE",  "0" );
        $targets->setAttribute( $target, "PROC_MASTER_TYPE", "ACTING_MASTER" );
    }
    else
    {
        $targets->setAttribute( $target, "PROC_MASTER_TYPE",
            "MASTER_CANDIDATE" );
    }
    $targets->setAttributeField( $target, "SCOM_SWITCHES", "reserved",   "0" );
    $targets->setAttributeField( $target, "SCOM_SWITCHES", "useFsiScom", "0" );
    $targets->setAttributeField( $target, "SCOM_SWITCHES", "useInbandScom",
        "0" );
    $targets->setAttributeField( $target, "SCOM_SWITCHES", "useXscom", "1" );

    #--------------------------------------------------
    ## Setup BARs

    my $lognode = $targets->getAttribute( $target, "FABRIC_NODE_ID" );
    my $logid   = $targets->getAttribute( $target, "FABRIC_CHIP_ID" );
    $targets->setAttribute( $target, "FSP_BASE_ADDR", "0x0000000000000000" );
    $targets->setAttribute( $target, "PSI_BRIDGE_BASE_ADDR",
        "0x0000000000000000" );

    # Starts at 1024TB - 2GB, 1MB per proc
    my $bar = sprintf( "0x%016X",
        0x0003FFFF80000000 + 0x400000 * $lognode + 0x100000 * $logid );
    $targets->setAttribute( $target, "INTP_BASE_ADDR", $bar );

    # Starts at 1024TB - 7GB, 1MB per PHB (=4MB per proc)
    $bar = sprintf(
        "\n            0x%016X,0x%016X,\n        0x%016X,0x%016X\n\t\t",
        0x0003FFFE40000000 + 0x1000000 * $lognode + 0x400000 * $logid +
          0x100000 * 0,
        0x0003FFFE40000000 + 0x1000000 * $lognode + 0x400000 * $logid +
          0x100000 * 1,
        0x0003FFFE40000000 + 0x1000000 * $lognode + 0x400000 * $logid +
          0x100000 * 2,
        0x0003FFFE40000000 + 0x1000000 * $lognode + 0x400000 * $logid +
          0x100000 * 3
    );
    $targets->setAttribute( $target, "PHB_BASE_ADDRS", $bar );

    $bar = sprintf(
        "\n            0x%016X,0x%016X,\n            0x%016X,0x%016X\n\t\t",
        0x0003FF8000000000 + 0x800000000 * $lognode + 0x200000000 * $logid +
          0x80000000 * 0,
        0x0003FF8000000000 + 0x800000000 * $lognode + 0x200000000 * $logid +
          0x80000000 * 1,
        0x0003FF8000000000 + 0x800000000 * $lognode + 0x200000000 * $logid +
          0x80000000 * 2,
        0x0003FF8000000000 + 0x800000000 * $lognode + 0x200000000 * $logid +
          0x80000000 * 3
    );
    $targets->setAttribute( $target, "PCI_BASE_ADDRS_32", $bar );

    # Starts at 976TB, 64GB per PHB (=256GB per proc)
    $bar = sprintf(
        "\n            0x%016X,0x%016X,\n            0x%016X,0x%016X\n\t\t",
        0x0003D00000000000 + 0x10000000000 * $lognode + 0x4000000000 * $logid +
          0x1000000000 * 0,
        0x0003D00000000000 + 0x10000000000 * $lognode + 0x4000000000 * $logid +
          0x1000000000 * 1,
        0x0003D00000000000 + 0x10000000000 * $lognode + 0x4000000000 * $logid +
          0x1000000000 * 2,
        0x0003D00000000000 + 0x10000000000 * $lognode + 0x4000000000 * $logid +
          0x1000000000 * 3
    );
    $targets->setAttribute( $target, "PCI_BASE_ADDRS_64", $bar );

    # Starts at 1024TB - 3GB
    $bar = sprintf( "0x%016X\n",
        0x0003FFFF40000000 + 0x4000 * $lognode + 0x1000 * $logid );
    $targets->setAttribute( $target, "RNG_BASE_ADDR", $bar );

    # Starts at 992TB - 128GB per MCS/Centaur
    $bar = sprintf( "0x%016X",
        0x0003E00000000000 + 0x40000000000 * $lognode + 0x10000000000 *
          $logid );
    $targets->setAttribute( $target, "IBSCOM_PROC_BASE_ADDR", $bar );
}

#--------------------------------------------------
## MCS
##
##

sub processMcs
{
    my $targets      = shift;
    my $target       = shift;
    my $parentTarget = shift;
    my $type         = $targets->getType($target);
    if ( $type ne "MCS" )
    {
        return;
    }
    my $lognode = $targets->getAttribute( $parentTarget, "FABRIC_NODE_ID" );
    my $logid   = $targets->getAttribute( $parentTarget, "FABRIC_CHIP_ID" );

    my $mcs = $targets->getAttribute( $target, "MCS_NUM" );

    #IBSCOM address range starts at 0x0003E00000000000 (992 TB)
    #128GB per MCS/Centaur
    #Addresses assigned by logical node, not physical node
    my $mcsStr = sprintf( "0x%016X",
        0x0003E00000000000 + 0x40000000000 * $lognode + 0x10000000000 * $logid +
          0x2000000000 * $mcs );

    $targets->setAttribute( $target, "IBSCOM_MCS_BASE_ADDR", $mcsStr );
}

#--------------------------------------------------
## ABUS
##
## Finds ABUS connections and creates PEER TARGET attributes

sub processAbus
{
    my $targets = shift;
    my $target  = shift;
    my $type    = $targets->getBusType($target);
    if ( $type ne "ABUS" )
    {
        return;
    }

    my $abus_child_conn = $targets->getFirstConnectionDestination($target);
    if ( $abus_child_conn ne "" )
    {
        ## set attributes for both directions
        my $aff1 = $targets->getAttribute( $target,          "AFFINITY_PATH" );
        my $aff2 = $targets->getAttribute( $abus_child_conn, "AFFINITY_PATH" );
        $targets->setAttribute( $abus_child_conn, "PEER_TARGET",
            $targets->getAttribute( $target, "PHYS_PATH" ) );
        $targets->setAttribute( $target, "PEER_TARGET",
            $targets->getAttribute( $abus_child_conn, "PHYS_PATH" ) );
        $targets->setAttribute( $abus_child_conn, "PEER_PATH",
            $targets->getAttribute( $target, "PHYS_PATH" ) );
        $targets->setAttribute( $target, "PEER_PATH",
            $targets->getAttribute( $abus_child_conn, "PHYS_PATH" ) );

        # copy Abus attributes to proc
        my $abus = $targets->getFirstConnectionBus($target);
        $targets->setAttribute( $target, "EI_BUS_TX_LANE_INVERT",
            $abus->{bus_attribute}->{SOURCE_TX_LANE_INVERT}->{default} );
        $targets->setAttribute( $target, "EI_BUS_TX_MSBSWAP",
            $abus->{bus_attribute}->{SOURCE_TX_MSBSWAP}->{default} );
        $targets->setAttribute( $abus_child_conn, "EI_BUS_TX_LANE_INVERT",
            $abus->{bus_attribute}->{DEST_TX_LANE_INVERT}->{default} );
        $targets->setAttribute( $abus_child_conn, "EI_BUS_TX_MSBSWAP",
            $abus->{bus_attribute}->{DEST_TX_MSBSWAP}->{default} );
    }
}

#--------------------------------------------------
## FSI
##
## Finds FSI connections and creates FSI MASTER attributes at endpoint target

sub processFsi
{
    my $targets      = shift;
    my $target       = shift;
    my $parentTarget = shift;
    my $type         = $targets->getBusType($target);
    if ( $type ne "FSIM" && $type ne "FSICM" )
    {
        return;
    }
    ## fsi can only have 1 connection
    my $fsi_child_conn = $targets->getFirstConnectionDestination($target);

    ## found something on other end
    if ( $fsi_child_conn ne "" )
    {
        my $fsi_link = $targets->getAttribute( $target, "FSI_LINK" );
        my $fsi_port = $targets->getAttribute( $target, "FSI_PORT" );

        my $cmfsi = $targets->getAttribute( $target, "CMFSI" );

        my $flipPort         = 0;
        my $fsi_child_target = $targets->getTargetParent($fsi_child_conn);
        my $child_type       = $targets->getType($fsi_child_target);

        $targets->setAttribute( $fsi_child_target, "FSI_MASTER_TYPE",
            "NO_MASTER" );
        if ( $type eq "FSIM" )
        {
            $targets->setAttribute( $fsi_child_target, "FSI_MASTER_TYPE",
                "MFSI" );
        }
        if ( $type eq "FSICM" )
        {
            $targets->setAttribute( $fsi_child_target, "FSI_MASTER_TYPE",
                "CMFSI" );
        }
        $targets->setAttribute( $fsi_child_target, "FSI_MASTER_CHIP",
            "physical:sys" );
        $targets->setAttribute( $fsi_child_target, "FSI_MASTER_PORT", "0xFF" );
        $targets->setAttribute( $fsi_child_target, "ALTFSI_MASTER_CHIP",
            "physical:sys" );
        $targets->setAttribute( $fsi_child_target, "ALTFSI_MASTER_PORT",
            "0xFF" );
        $targets->setAttribute( $fsi_child_target, "FSI_SLAVE_CASCADE", "0" );

        my $phys_path = $targets->getAttribute( $parentTarget, "PHYS_PATH" );
        if ( $cmfsi == 0 )
        {
            $targets->setAttribute( $fsi_child_target, "FSI_MASTER_CHIP",
                $phys_path );
            $targets->setAttribute( $fsi_child_target, "FSI_MASTER_PORT",
                $fsi_port );
        }
        else
        {
            $targets->setAttribute( $fsi_child_target, "ALTFSI_MASTER_CHIP",
                $phys_path );
            $targets->setAttribute( $fsi_child_target, "ALTFSI_MASTER_PORT",
                $fsi_port );
        }
        $targets->setAttributeField( $fsi_child_target, "FSI_OPTION_FLAGS",
            "flipPort", $flipPort );
        $targets->setAttributeField( $fsi_child_target, "FSI_OPTION_FLAGS",
            "reserved", "0" );
    }
}

#--------------------------------------------------
## PCIE
##
## Creates attributes from abstract PCI attributes on bus

sub processPcie
{
    my $targets      = shift;
    my $target       = shift;
    my $parentTarget = shift;
    my $type         = $targets->getMrwType($target);

    ## process pcie config target
    ## this is a special target whose children are the different ways
    ## to configure iop/phb's
    if ( $type ne "PCI_CONFIG" )
    {
        return;
    }
    ## Get config children
    my @lane_swap;
    $lane_swap[0][0] = 0;
    $lane_swap[0][1] = 0;
    $lane_swap[1][0] = 0;
    $lane_swap[1][1] = 0;

    my @lane_mask;
    $lane_mask[0][0] = "0x0000";
    $lane_mask[0][1] = "0x0000";
    $lane_mask[1][0] = "0x0000";
    $lane_mask[1][1] = "0x0000";

    my @lane_rev;
    $lane_rev[0][0] = 0;
    $lane_rev[0][1] = 0;
    $lane_rev[1][0] = 0;
    $lane_rev[1][1] = 0;

    my @is_slot;
    $is_slot[0][0] = 0;
    $is_slot[0][1] = 0;
    $is_slot[1][0] = 0;
    $is_slot[1][1] = 0;

    my $phb_config = "00000000";

    my %cfg_check;
    foreach my $child ( @{ $targets->getTargetChildren($target) } )
    {
        my $num_connections = $targets->getNumConnections($child);
        if ( $num_connections > 0 )
        {
            my $pci_endpoint = $targets->getFirstConnectionDestination($child);
            my $pci_endpoint_type =
              $targets->getAttribute( $targets->getTargetParent($pci_endpoint),
                "CLASS" );

            my $bus     = $targets->getConnectionBus( $target, 0 );
            my $iop_num = $targets->getAttribute( $child,      "IOP_NUM" );
            my $phb_num = $targets->getAttribute( $child,      "PHB_NUM" );
            my $num_lanes = $targets->getAttribute( $child, "PCIE_NUM_LANES" );
            my $lane_set  = $targets->getAttribute( $child, "PCIE_LANE_SET" );
            my $capi      = $targets->getAttribute( $child, "ENABLE_CAPI" );
            my $config_num =
              $targets->getAttribute( $child, "PCIE_CONFIG_NUM" );

            if ( $pci_endpoint_type eq "CARD" )
            {
                $is_slot[$iop_num][$lane_set] = 1;
            }
            $lane_swap[$iop_num][$lane_set] =
              $targets->getBusAttribute( $child, 0, "LANE_SWAP" );
            $lane_mask[$iop_num][$lane_set] =
              $targets->getAttribute( $child, "PCIE_LANE_MASK" );
            $lane_rev[$iop_num][$lane_set] =
              $targets->getBusAttribute( $child, 0, "LANE_REVERSAL" );
            ## check to make sure more than 1 config is not used
            if ( $cfg_check{$iop_num} ne "" )
            {
                if ( $cfg_check{$iop_num} != $config_num )
                {
                    die
"ERROR: only 1 pcie config num may be used for each iop\n";
                }
            }
            $cfg_check{$iop_num} = $config_num;
            substr( $phb_config, $phb_num, 1, "1" );
        }
    }
    my $hex = sprintf( '%X', oct("0b$phb_config") );

    $targets->setAttribute( $parentTarget, "PROC_PCIE_PHB_ACTIVE",
        "0x" . $hex );
    my $lane_mask_attr = sprintf( "%s,%s,%s,%s",
        $lane_mask[0][0], $lane_mask[0][1],
        $lane_mask[1][0], $lane_mask[1][1] );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_LANE_MASK",
        $lane_mask_attr );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_LANE_MASK_NON_BIFURCATED",
        $lane_mask_attr );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_LANE_MASK_BIFURCATED",
        "0,0,0,0" );

    my $lane_swap_attr = sprintf( "%s,%s,%s,%s",
        $lane_swap[0][0], $lane_swap[0][1],
        $lane_swap[1][0], $lane_swap[1][1] );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_IOP_SWAP",
        $lane_swap_attr );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_IOP_SWAP_NON_BIFURCATED",
        $lane_swap_attr );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_IOP_SWAP_BIFURCATED",
        "0,0,0,0" );

    my $lane_rev_attr = sprintf( "%s,%s,%s,%s",
        $lane_rev[0][0], $lane_rev[0][1], $lane_rev[1][0], $lane_rev[1][1] );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_IOP_REVERSAL",
        $lane_rev_attr );
    $targets->setAttribute( $parentTarget,
        "PROC_PCIE_IOP_REVERSAL_NON_BIFURCATED",
        $lane_rev_attr );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_IOP_REVERSAL_BIFURCATED",
        "0,0,0,0" );

    my $is_slot_attr = sprintf( "%s,%s,%s,%s",
        $is_slot[0][0], $is_slot[0][1], $is_slot[1][0], $is_slot[1][1] );
    $targets->setAttribute( $parentTarget, "PROC_PCIE_IS_SLOT", $is_slot_attr );

    ## don't support DSMP
    $targets->setAttribute( $parentTarget, "PROC_PCIE_DSMP_CAPABLE",
        "0,0,0,0" );
}

#--------------------------------------------------
## MEMBUF
##
## Finds I2C connections to DIMM and creates EEPROM attributes
## FYI:  I had to handle DMI busses in framework because they
## define affinity path

sub processMembuf
{
    my $targets = shift;
    my $target  = shift;
    my $type    = $targets->getType($target);

    if ( $type ne "MEMBUF" )
    {
        return;
    }

  #my $gpio_expander=$targets->findEndpoint($target,"I2C","GPIO_EXPANDER");
  #my $vreg = $targets->findEndpoint($gpio_expander,"GPIO","VOLTAGE_REGULATOR");
  #my $pwr = $targets->findEndpoint($vreg,"POWER","MEMBUF");

    my %mba_port_map;
    foreach my $child ( @{ $targets->getTargetChildren($target) } )
    {
        my $child_type     = $targets->getType($child);
        my $child_bus_type = $targets->getBusType($child);

        ## I2C children of membuf
        if ( $child_bus_type eq "I2C" )
        {
            ## get what is connected to this I2C
            for ( my $i = 0 ; $i < $targets->getNumConnections($child) ; $i++ )
            {
                my $conn_target =
                  $targets->getConnectionDestination( $child, $i );
                my $conn_parent = $targets->getTargetParent($conn_target);
                my $i2cslave    = $targets->getTargetParent($conn_parent);

                my $conn_type     = $targets->getType($i2cslave);
                my $conn_mrw_type = $targets->getMrwType($conn_parent);
                my $addr          =
                  $targets->getBusAttribute( $child, $i, "I2C_ADDRESS" );
                if ( $addr eq "" )
                {
                    die
" ERROR: Bus I2C ADDRESS cannot be empty ($child,$i2cslave)\n";
                }
                my $port   = $targets->getAttribute( $child, "I2C_PORT" );
                my $engine = $targets->getAttribute( $child, "I2C_ENGINE" );

                ## found a DIMM
                my $phys_path_empty =
                  $targets->isBadAttribute( $target, "PHYS_PATH", "" );
                if ( !$phys_path_empty )
                {
                    my $path = $targets->getAttribute( $target, "PHYS_PATH" );
                    if ( $conn_type eq "DIMM" )
                    {
                        setEepromAttributes(
                            $targets,  "EEPROM_VPD_PRIMARY_INFO",
                            $i2cslave, $path,
                            $port,     $addr,
                            $engine,   "0x01",
                            "0x01",    "0x00",
                            "0x05",    ""
                        );
                        setEepromAttributes(
                            $targets,  "EEPROM_VPD_FRU_INFO",
                            $i2cslave, $path,
                            $port,     $addr,
                            $engine,   "0x01",
                            "0x01",    "0x00",
                            "0x05",    "0++"
                        );
                    }
                }
            }
        }
        ## MBA children of membuf
        if ( $child_type eq "MBA" )
        {
            ##get attached dimms
            for ( my $i = 0 ; $i < $targets->getNumConnections($child) ; $i++ )
            {
                my $conn_target =
                  $targets->getConnectionDestination( $child, $i );
                my $dimm = $targets->getTargetParent($conn_target);

                ## create mba dimm and port numbers
                if ( $mba_port_map{$child}{port} eq "" )
                {
                    $mba_port_map{$child}{port} = 0;
                }
                if ( $mba_port_map{$child}{dimm} eq "" )
                {
                    $mba_port_map{$child}{dimm} = 0;
                }
                $targets->setAttribute( $dimm, "MBA_DIMM",
                    $mba_port_map{$child}{dimm} );
                $targets->setAttribute( $dimm, "MBA_PORT",
                    $mba_port_map{$child}{port} );

                if ( $mba_port_map{$child}{port} == 0 )
                {
                    $mba_port_map{$child}{port}++;
                }
                elsif ( $mba_port_map{$child}{dimm} == 0 )
                {
                    $mba_port_map{$child}{port} = 0;
                    $mba_port_map{$child}{dimm}++;
                }
                else
                {
                    $mba_port_map{$child}{port} = 0;
                    $mba_port_map{$child}{dimm} = 0;
                }
            }
        }
    }
}

sub setEepromAttributes
{
    my $targets = shift;
    my $name    = shift;
    my $target  = shift;
    my $path    = shift;
    my $port    = shift;
    my $addr    = shift;
    my $engine  = shift;
    my $offset  = shift;
    my $mem     = shift;
    my $page    = shift;
    my $cycle   = shift;
    my $fru     = shift;

    $targets->setAttributeField( $target, $name, "i2cMasterPath",   $path );
    $targets->setAttributeField( $target, $name, "port",            $port );
    $targets->setAttributeField( $target, $name, "devAddr",         $addr );
    $targets->setAttributeField( $target, $name, "engine",          $engine );
    $targets->setAttributeField( $target, $name, "byteAddrOffset",  $offset );
    $targets->setAttributeField( $target, $name, "maxMemorySizeKB", $mem );
    $targets->setAttributeField( $target, $name, "writePageSize",   $page );
    $targets->setAttributeField( $target, $name, "writeCycleTime",  $cycle );
    if ( $fru ne "" )
    {
        $targets->setAttributeField( $target, $name, "fruId", $fru );
    }
}

sub printUsage
{
    print "
processMrwl.pl -x [XML filename] [OPTIONS]
Options:
        -f = force output file creation even when errors
        -d = debug mode
        -v = version
";
    exit(1);
}
