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

if ($version == 1)
{
    die "\nprocessMrw.pl\tversion $VERSION\n";
}

if ($serverwiz_file eq "")
{
    printUsage();
}

$XML::Simple::PREFERRED_PARSER = 'XML::Parser';

my $targetObj = Targets->new;
if ($force == 1) { $targetObj->{force} = 1; }
if ($debug == 1) { $targetObj->{debug} = 1; }
$targetObj->setVersion($VERSION);
my $xmldir = dirname($serverwiz_file);
$targetObj->loadXML($serverwiz_file);

#--------------------------------------------------
## loop through all targets and do stuff
foreach my $target (sort keys %{ $targetObj->getAllTargets() })
{
    my $type = $targetObj->getType($target);
    if ($type eq "SYS")
    {
        processSystem($targetObj, $target);
    } 
    elsif ($type eq "PROC")
    {
        processProcessor($targetObj, $target);
    }
    elsif ($type eq "MEMBUF")
    {
        processMembuf($targetObj, $target);
    }
}

## check for errors
foreach my $target (keys %{ $targetObj->getAllTargets() })
{
    errorCheck($targetObj, $target);
}

#--------------------------------------------------
## write out final XML
my $xml_fh;
my $filename = $xmldir . "/" . $targetObj->getSystemName() . "_hb.mrw.xml";
print "Creating XML: $filename\n";
open($xml_fh, ">$filename");
$targetObj->printXML($xml_fh, "top");
close $xml_fh;
if (!$targetObj->{errorsExist})
{
    print "MRW created successfully!\n";
}

#--------------------------------------------------
#--------------------------------------------------
## Processing subroutines


#--------------------------------------------------
## System
##

sub processSystem
{
    my $targetObj = shift;
    my $target    = shift;

    $targetObj->setAttribute($target, "MAX_MCS_PER_SYSTEM",
        $targetObj->{NUM_PROCS_PER_NODE} * $targetObj->{MAX_MCS});
    $targetObj->setAttribute($target, "MAX_PROC_CHIPS_PER_NODE",
        $targetObj->{NUM_PROCS_PER_NODE});
}

#--------------------------------------------------
## Processor
##

sub processProcessor
{
    my $targetObj = shift;
    my $target    = shift;

    #########################
    ## Copy PCIE attributes from socket
    ## In serverwiz, processor instances are not unique
    ## because plugged into socket
    ## so processor instance unique attributes are socket level.
    ## The grandparent is guaranteed to be socket.
    my $socket_target =
       $targetObj->getTargetParent($targetObj->getTargetParent($target));
    $targetObj->copyAttribute($socket_target,$target,"LOCATION_CODE");
    $targetObj->copyAttribute($socket_target,$target,"FRU_ID");
    $targetObj->copyAttribute($socket_target,$target,"IPMI_SENSORS");
    
    foreach my $attr (sort (keys
           %{ $targetObj->getTarget($socket_target)->{TARGET}->{attribute} }))
    {
        if ($attr =~ /PROC\_PCIE/)
        {
            $targetObj->copyAttribute($socket_target,$target,$attr);
        }
    }
    $targetObj->log($target, "Processing PROC");
    foreach my $child (@{ $targetObj->getTargetChildren($target) })
    {
        $targetObj->log($target, "Processing PROC child: $child");
        my $child_type = $targetObj->getType($child);
        if ($child_type eq "NA" || $child_type eq "FSI")
        {
            $child_type = $targetObj->getMrwType($child);
        }
        if ($child_type eq "ABUS")
        {
            processAbus($targetObj, $child);
        }
        elsif ($child_type eq "FSIM" || $child_type eq "FSICM")
        {
            processFsi($targetObj, $child, $target);
        }
        elsif ($child_type eq "PCI_CONFIG")
        {
            processPcie($targetObj, $child, $target);
        }
        elsif ($child_type eq "MCS")
        {
            processMcs($targetObj, $child, $target);
        }
        elsif ($child_type eq "OCC")
        {
            processOcc($targetObj, $child, $target);
        }
    }
    
    ## update path for mvpd's and sbe's
    my $path  = $targetObj->getAttribute($target, "PHYS_PATH");
    my $model = $targetObj->getAttribute($target, "MODEL");
    
    $targetObj->setAttributeField($target,
        "EEPROM_VPD_PRIMARY_INFO","i2cMasterPath",$path);
    $targetObj->setAttributeField($target,
        "EEPROM_VPD_BACKUP_INFO","i2cMasterPath",$path);
    $targetObj->setAttributeField($target,
        "EEPROM_SBE_PRIMARY_INFO","i2cMasterPath",$path);
    $targetObj->setAttributeField($target,
        "EEPROM_SBE_BACKUP_INFO","i2cMasterPath",$path);
    $targetObj->setAttributeField($target,
        "EEPROM_VPD_FRU_INFO","i2cMasterPath",$path);
                                
    ## initialize master processor FSI's
    $targetObj->setAttributeField($target, "FSI_OPTION_FLAGS", "flipPort", "1");

    if ($target eq $targetObj->getMasterProc())
    {
        $targetObj->setAttributeField($target, "FSI_OPTION_FLAGS", "reserved",
            "0");
        $targetObj->setAttribute($target, "FSI_MASTER_CHIP",    "physical:sys");
        $targetObj->setAttribute($target, "FSI_MASTER_PORT",    "0xFF");
        $targetObj->setAttribute($target, "ALTFSI_MASTER_CHIP", "physical:sys");
        $targetObj->setAttribute($target, "ALTFSI_MASTER_PORT", "0xFF");
        $targetObj->setAttribute($target, "FSI_MASTER_TYPE",    "NO_MASTER");
        $targetObj->setAttribute($target, "FSI_SLAVE_CASCADE",  "0");
        $targetObj->setAttribute($target, "PROC_MASTER_TYPE", "ACTING_MASTER");
    }
    else
    {
        $targetObj->setAttribute($target, "PROC_MASTER_TYPE",
            "MASTER_CANDIDATE");
    }
    $targetObj->setAttributeField($target, "SCOM_SWITCHES", "reserved",   "0");
    $targetObj->setAttributeField($target, "SCOM_SWITCHES", "useFsiScom", "0");
    $targetObj->setAttributeField($target, "SCOM_SWITCHES", "useInbandScom",
        "0");
    $targetObj->setAttributeField($target, "SCOM_SWITCHES", "useXscom", "1");
    
    processMembufVpdAssociation($targetObj,$target);
    setupBars($targetObj,$target);
}

################################
## Setup address map

sub setupBars
{
    my $targetObj = shift;
    my $target = shift;
    #--------------------------------------------------
    ## Setup BARs

    my $node = $targetObj->getAttribute($target, "FABRIC_NODE_ID");
    my $proc   = $targetObj->getAttribute($target, "FABRIC_CHIP_ID");
    
    my @bars=("FSP_BASE_ADDR","PSI_BRIDGE_BASE_ADDR",
              "INTP_BASE_ADDR","PHB_BASE_ADDRS","PCI_BASE_ADDRS_32",
              "PCI_BASE_ADDRS_64","RNG_BASE_ADDR","IBSCOM_PROC_BASE_ADDR");
    
    foreach my $bar (@bars)
    {
        my ($num,$base,$node_offset,$proc_offset,$offset) = split(/,/,
               $targetObj->getAttribute($target,$bar));
        my $i_base = Math::BigInt->new($base);
        my $i_node_offset = Math::BigInt->new($node_offset);
        my $i_proc_offset = Math::BigInt->new($proc_offset);
        my $i_offset = Math::BigInt->new($offset);
        
        my $value="";
        if ($num==0)
        {
            $value=$base;
        }
        else
        {
            for (my $i=0;$i<$num;$i++)
            {
                my $b=sprintf("0x%016X",
         $i_base+$i_node_offset*$node+$i_proc_offset*$proc+$i_offset*$i);
                my $sep=",";
                if ($i==$num-1) 
                {
                    $sep="";
                }
                $value=$value.$b.$sep;
            }
            
        }
        $targetObj->setAttribute($target,$bar,$value);
    }
}

#--------------------------------------------------
## MCS
##
sub processMcs
{
    my $targetObj    = shift;
    my $target       = shift;
    my $parentTarget = shift;

    my $node = $targetObj->getAttribute($parentTarget, "FABRIC_NODE_ID");
    my $proc   = $targetObj->getAttribute($parentTarget, "FABRIC_CHIP_ID");

    my ($base,$node_offset,$proc_offset,$offset) = split(/,/,
               $targetObj->getAttribute($target,"IBSCOM_MCS_BASE_ADDR"));
    my $i_base = Math::BigInt->new($base);
    my $i_node_offset = Math::BigInt->new($node_offset);
    my $i_proc_offset = Math::BigInt->new($proc_offset);
    my $i_offset = Math::BigInt->new($offset);
    
    
    my $mcs = $targetObj->getAttribute($target, "MCS_NUM");
    my $mcsStr=sprintf("0x%016X",   
         $i_base+$i_node_offset*$node+$i_proc_offset*$proc+$i_offset*$mcs);
    $targetObj->setAttribute($target, "IBSCOM_MCS_BASE_ADDR", $mcsStr);
}

#--------------------------------------------------
## ABUS
##
## Finds ABUS connections and creates PEER TARGET attributes

sub processAbus
{
    my $targetObj = shift;
    my $target    = shift;

    my $abus_child_conn = $targetObj->getFirstConnectionDestination($target);
    if ($abus_child_conn ne "")
    {
        ## set attributes for both directions
        my $aff1 = $targetObj->getAttribute($target, "AFFINITY_PATH");
        my $aff2 = $targetObj->getAttribute($abus_child_conn, "AFFINITY_PATH");
        $targetObj->setAttribute($abus_child_conn, "PEER_TARGET",
            $targetObj->getAttribute($target, "PHYS_PATH"));
        $targetObj->setAttribute($target, "PEER_TARGET",
            $targetObj->getAttribute($abus_child_conn, "PHYS_PATH"));
        $targetObj->setAttribute($abus_child_conn, "PEER_PATH",
            $targetObj->getAttribute($target, "PHYS_PATH"));
        $targetObj->setAttribute($target, "PEER_PATH",
            $targetObj->getAttribute($abus_child_conn, "PHYS_PATH"));

        # copy Abus attributes to proc
        my $abus = $targetObj->getFirstConnectionBus($target);
        $targetObj->setAttribute($target, "EI_BUS_TX_LANE_INVERT",
            $abus->{bus_attribute}->{SOURCE_TX_LANE_INVERT}->{default});
        $targetObj->setAttribute($target, "EI_BUS_TX_MSBSWAP",
            $abus->{bus_attribute}->{SOURCE_TX_MSBSWAP}->{default});
        $targetObj->setAttribute($abus_child_conn, "EI_BUS_TX_LANE_INVERT",
            $abus->{bus_attribute}->{DEST_TX_LANE_INVERT}->{default});
        $targetObj->setAttribute($abus_child_conn, "EI_BUS_TX_MSBSWAP",
            $abus->{bus_attribute}->{DEST_TX_MSBSWAP}->{default});
    }
}

#--------------------------------------------------
## FSI
##
## Finds FSI connections and creates FSI MASTER attributes at endpoint target

sub processFsi
{
    my $targetObj    = shift;
    my $target       = shift;
    my $parentTarget = shift;
    my $type         = $targetObj->getBusType($target);

    ## fsi can only have 1 connection
    my $fsi_child_conn = $targetObj->getFirstConnectionDestination($target);

    ## found something on other end
    if ($fsi_child_conn ne "")
    {
        my $fsi_link = $targetObj->getAttribute($target, "FSI_LINK");
        my $fsi_port = $targetObj->getAttribute($target, "FSI_PORT");

        my $cmfsi = $targetObj->getAttribute($target, "CMFSI");

        my $flipPort         = 0;
        my $fsi_child_target = $targetObj->getTargetParent($fsi_child_conn);
        my $child_type       = $targetObj->getType($fsi_child_target);

        $targetObj->setAttribute($fsi_child_target, "FSI_MASTER_TYPE",
            "NO_MASTER");
        if ($type eq "FSIM")
        {
            $targetObj->setAttribute($fsi_child_target, "FSI_MASTER_TYPE",
                "MFSI");
        }
        if ($type eq "FSICM")
        {
            $targetObj->setAttribute($fsi_child_target, "FSI_MASTER_TYPE",
                "CMFSI");
        }
        $targetObj->setAttribute($fsi_child_target, "FSI_MASTER_CHIP",
            "physical:sys");
        $targetObj->setAttribute($fsi_child_target, "FSI_MASTER_PORT", "0xFF");
        $targetObj->setAttribute($fsi_child_target, "ALTFSI_MASTER_CHIP",
            "physical:sys");
        $targetObj->setAttribute($fsi_child_target, "ALTFSI_MASTER_PORT",
            "0xFF");
        $targetObj->setAttribute($fsi_child_target, "FSI_SLAVE_CASCADE", "0");

        my $phys_path = $targetObj->getAttribute($parentTarget, "PHYS_PATH");
        
        if ($cmfsi == 0)
        {
            $targetObj->setAttribute($fsi_child_target, "FSI_MASTER_CHIP",
                $phys_path);
            $targetObj->setAttribute($fsi_child_target, "FSI_MASTER_PORT",
                $fsi_port);
            print 
        }
        else
        {
            $targetObj->setAttribute($fsi_child_target, "ALTFSI_MASTER_CHIP",
                $phys_path);
            $targetObj->setAttribute($fsi_child_target, "ALTFSI_MASTER_PORT",
                $fsi_port);
        }
        $targetObj->setAttributeField($fsi_child_target, "FSI_OPTION_FLAGS",
            "flipPort", $flipPort);
        $targetObj->setAttributeField($fsi_child_target, "FSI_OPTION_FLAGS",
            "reserved", "0");
    }
}

#--------------------------------------------------
## PCIE
##
## Creates attributes from abstract PCI attributes on bus

sub processPcie
{
    my $targetObj    = shift;
    my $target       = shift;
    my $parentTarget = shift;
    

    ## process pcie config target
    ## this is a special target whose children are the different ways
    ## to configure iop/phb's

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
    my @equalization;
    foreach my $child (@{ $targetObj->getTargetChildren($target) })
    {
        my $num_connections = $targetObj->getNumConnections($child);
        if ($num_connections > 0)
        {
            my $pci_endpoint =$targetObj->getFirstConnectionDestination($child);
            my $pci_endpoint_type =
              $targetObj->getAttribute(
                $targetObj->getTargetParent($pci_endpoint), "CLASS");

            my $bus = $targetObj->getConnectionBus($target, 0);
            my $iop_num = $targetObj->getAttribute($child, "IOP_NUM");
            my $phb_num = $targetObj->getAttribute($child, "PHB_NUM");
            my $num_lanes = $targetObj->getAttribute($child, "PCIE_NUM_LANES");
            my $lane_set = $targetObj->getAttribute($child, "PCIE_LANE_SET");
            my $capi = $targetObj->getAttribute($child, "ENABLE_CAPI");
            my $config_num =  
               $targetObj->getAttribute($child, "PCIE_CONFIG_NUM");

            if ($pci_endpoint_type eq "CARD")
            {
                $is_slot[$iop_num][$lane_set] = 1;
            }
            $lane_swap[$iop_num][$lane_set] =
              $targetObj->getBusAttribute($child, 0, "LANE_SWAP");
            $lane_mask[$iop_num][$lane_set] =
              $targetObj->getAttribute($child, "PCIE_LANE_MASK");
            $lane_rev[$iop_num][$lane_set] =
              $targetObj->getBusAttribute($child, 0, "LANE_REVERSAL");
            $equalization[$phb_num] = $targetObj->getBusAttribute($child, 0, 
              "PROC_PCIE_LANE_EQUALIZATION");  

            ## check to make sure more than 1 config is not used
            if ($cfg_check{$iop_num} ne "")
            {
                if ($cfg_check{$iop_num} != $config_num)
                {
                    die
"ERROR: only 1 pcie config num may be used for each iop\n";
                }
            }
            $cfg_check{$iop_num} = $config_num;
            substr($phb_config, $phb_num, 1, "1");
        }
    }
    my $hex = sprintf('%X', oct("0b$phb_config"));

    $targetObj->setAttribute($parentTarget, "PROC_PCIE_PHB_ACTIVE","0x" . $hex);
    my $lane_mask_attr = sprintf("%s,%s,%s,%s",
        $lane_mask[0][0], $lane_mask[0][1],
        $lane_mask[1][0], $lane_mask[1][1]);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_LANE_MASK",
        $lane_mask_attr);
    $targetObj->setAttribute($parentTarget,"PROC_PCIE_LANE_MASK_NON_BIFURCATED",
        $lane_mask_attr);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_LANE_MASK_BIFURCATED",
        "0,0,0,0");

    my $lane_swap_attr = sprintf("%s,%s,%s,%s",
        $lane_swap[0][0], $lane_swap[0][1],
        $lane_swap[1][0], $lane_swap[1][1]);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_IOP_SWAP",
        $lane_swap_attr);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_IOP_SWAP_NON_BIFURCATED",
        $lane_swap_attr);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_IOP_SWAP_BIFURCATED",
        "0,0,0,0");

    my $lane_rev_attr = sprintf("%s,%s,%s,%s",
        $lane_rev[0][0], $lane_rev[0][1], $lane_rev[1][0], $lane_rev[1][1]);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_IOP_REVERSAL",
        $lane_rev_attr);
    $targetObj->setAttribute($parentTarget,
        "PROC_PCIE_IOP_REVERSAL_NON_BIFURCATED",
        $lane_rev_attr);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_IOP_REVERSAL_BIFURCATED",
        "0,0,0,0");

    my $is_slot_attr = sprintf("%s,%s,%s,%s",
        $is_slot[0][0], $is_slot[0][1], $is_slot[1][0], $is_slot[1][1]);
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_IS_SLOT", $is_slot_attr);

    ## don't support DSMP
    $targetObj->setAttribute($parentTarget, "PROC_PCIE_DSMP_CAPABLE","0,0,0,0");
    
    my $equalization_str=join(',',@equalization);
    $targetObj->setAttribute($parentTarget,"PROC_PCIE_LANE_EQUALIZATION",
         $equalization_str);
    
}
#--------------------------------------------------
## OCC
##
sub processOcc
{
    my $targetObj    = shift;
    my $target       = shift;
    my $parentTarget = shift;
    my $master_capable=0;
    if ($parentTarget eq $targetObj->getMasterProc())
    {
        $master_capable=1;
    }
    $targetObj->setAttribute($target,"OCC_MASTER_CAPABLE",$master_capable);
}

sub processMembufVpdAssociation
{
    my $targetObj = shift;
    my $target    = shift;
    
    my $vpds=$targetObj->findConnections($target,"I2C","VPD");
    if ($vpds ne "" ) {
        my $vpd = $vpds->{CONN}->[0];

        my $membuf_assocs=$targetObj->findConnections($vpd->{DEST_PARENT},
                          "LOGICAL_ASSOCIATION","MEMBUF");
        if ($membuf_assocs ne "") {
            foreach my $membuf_assoc (@{$membuf_assocs->{CONN}}) {
                my $membuf_target = $membuf_assoc->{DEST_PARENT};
                setEepromAttributes($targetObj,
                       "EEPROM_VPD_PRIMARY_INFO",$membuf_target,$vpd);
            }
        }
    }
    
}
#--------------------------------------------------
## MEMBUF
##
## Finds I2C connections to DIMM and creates EEPROM attributes
## FYI:  I had to handle DMI busses in framework because they
## define affinity path

sub processMembuf
{
    my $targetObj = shift;
    my $target    = shift;
    if ($targetObj->isBadAttribute($target, "PHYS_PATH", "")) 
    { 
        ##dmi is probably not connected.  will get caught in error checking
        return;
    }
    processMembufVpdAssociation($targetObj,$target);
      
    ## finds which gpio expander that controls vddr regs for membufs
    my $gpioexp=$targetObj->findConnections($target,"I2C","GPIO_EXPANDER");
    if ($gpioexp ne "" ) {
        my $vreg=$targetObj->findConnections(
            $gpioexp->{CONN}->[0]->{DEST_PARENT},"GPIO","VOLTAGE_REGULATOR");
        if ($vreg ne "") {
            my $vddPin = $targetObj->getAttribute(
                 $vreg->{CONN}->[0]->{SOURCE},"CHIP_UNIT");
            my $membufs=$targetObj->findConnections(
               $vreg->{CONN}->[0]->{DEST_PARENT},"POWER","MEMBUF");
            if ($membufs ne "") {
                foreach my $membuf (@{$membufs->{CONN}}) {
                    my $aff = $targetObj->getAttribute($membuf->{DEST_PARENT},
                        "PHYS_PATH");
                    setGpioAttributes($targetObj,$membuf->{DEST_PARENT},
                        $gpioexp->{CONN}->[0],$vddPin);

                }
            }
        }
    }
    ## Process MEMBUF to DIMM I2C connections
    my $dimms=$targetObj->findConnections($target,"I2C","SPD");
    if ($dimms ne "") {
        foreach my $dimm (@{$dimms->{CONN}}) {
            my $dimm_target = $targetObj->getTargetParent($dimm->{DEST_PARENT});
            setEepromAttributes($targetObj,
                       "EEPROM_VPD_PRIMARY_INFO",$dimm_target,
                       $dimms->{CONN}->[0]);
            setEepromAttributes($targetObj,
                       "EEPROM_VPD_FRU_INFO",$dimm_target,
                       $dimms->{CONN}->[0],"0++");
        }
    }
    ## Do MBA port mapping
    my %mba_port_map;
    my $ddrs=$targetObj->findConnections($target,"DDR3","DIMM");
    if ($ddrs ne "") {
        my %portmap;
        foreach my $ddr (@{$ddrs->{CONN}}) {
            my $mba=$ddr->{SOURCE};
            my $dimm=$ddr->{DEST_PARENT};
            my ($dimmnum,$port)=split(//,sprintf("%02b\n",$portmap{$mba}));        
            $targetObj->setAttribute($dimm, "MBA_DIMM",$dimmnum);
            $targetObj->setAttribute($dimm, "MBA_PORT",$port);
            $portmap{$mba}++;
            
            ## Copy connector attributes
            my $dimmconn=$targetObj->getTargetParent($dimm);
            $targetObj->copyAttribute($dimmconn,$dimm,"IPMI_SENSORS");
        }
    }
}


sub setEepromAttributes
{
    my $targetObj = shift;
    my $name = shift;
    my $target = shift;
    my $conn_target = shift;
    my $fru = shift;
   
    my $port = $targetObj->getAttribute($conn_target->{SOURCE}, "I2C_PORT");
    my $engine = $targetObj->getAttribute($conn_target->{SOURCE}, "I2C_ENGINE");
    my $addr = $targetObj->getBusAttribute($conn_target->{SOURCE},
            $conn_target->{BUS_NUM}, "I2C_ADDRESS");
            
    my $path = $targetObj->getAttribute($conn_target->{SOURCE_PARENT}, 
               "PHYS_PATH");
    my $mem  = $targetObj->getAttribute($conn_target->{DEST_PARENT},
               "MEMORY_SIZE_IN_KB");
    my $cycle  = $targetObj->getAttribute($conn_target->{DEST_PARENT},
               "WRITE_CYCLE_TIME");
    my $page  = $targetObj->getAttribute($conn_target->{DEST_PARENT},
               "WRITE_PAGE_SIZE");
    my $offset  = $targetObj->getAttribute($conn_target->{DEST_PARENT},
               "BYTE_ADDRESS_OFFSET");
    
    $targetObj->setAttributeField($target, $name, "i2cMasterPath", $path);
    $targetObj->setAttributeField($target, $name, "port", $port);
    $targetObj->setAttributeField($target, $name, "devAddr", $addr);
    $targetObj->setAttributeField($target, $name, "engine", $engine);
    $targetObj->setAttributeField($target, $name, "byteAddrOffset", $offset);
    $targetObj->setAttributeField($target, $name, "maxMemorySizeKB", $mem);
    $targetObj->setAttributeField($target, $name, "writePageSize", $page);
    $targetObj->setAttributeField($target, $name, "writeCycleTime", $cycle);
    
    if ($fru ne "")
    {
        $targetObj->setAttributeField($target, $name, "fruId", $fru);
    }
}


sub setGpioAttributes
{
    my $targetObj = shift;
    my $target = shift;
    my $conn_target = shift;
    my $vddrPin = shift;

    my $port = $targetObj->getAttribute($conn_target->{SOURCE}, "I2C_PORT");
    my $engine = $targetObj->getAttribute($conn_target->{SOURCE}, "I2C_ENGINE");
    my $addr = $targetObj->getBusAttribute($conn_target->{SOURCE},
            $conn_target->{BUS_NUM}, "I2C_ADDRESS");
    my $path = $targetObj->getAttribute($conn_target->{SOURCE_PARENT}, 
               "PHYS_PATH");

    
    my $name="GPIO_INFO";
    $targetObj->setAttributeField($target, $name, "i2cMasterPath", $path);
    $targetObj->setAttributeField($target, $name, "port", $port);
    $targetObj->setAttributeField($target, $name, "devAddr", $addr);
    $targetObj->setAttributeField($target, $name, "engine", $engine);
    $targetObj->setAttributeField($target, $name, "vddrPin", $vddrPin);
}

#--------------------------------------------------
## ERROR checking
sub errorCheck
{
    my $targetObj = shift;
    my $target    = shift;
    my $type      = $targetObj->getType($target);

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
        'PHYS_PATH' =>'DMI connection is missing to this membuf from processor',
    );

    my @errors;
    if ($targetObj->getMasterProc() eq $target)
    {
        $type = "MASTER_PROC";
    }
    foreach my $attr (@{ $attribute_checks{$type} })
    {
        my ($a,         $v)     = split(/\|/, $attr);
        my ($a_complex, $field) = split(/\//, $a);
        if ($field ne "")
        {
            if ($targetObj->isBadComplexAttribute(
                    $target, $a_complex, $field, $v) )
            {
                push(@errors,sprintf(
                        "$a attribute is invalid (Target=%s)\n\t%s\n",
                        $target, $error_msg{$a}));
            }
        }
        else
        {
            if ($targetObj->isBadAttribute($target, $a, $v))
            {
                push(@errors,sprintf(
                        "$a attribute is invalid (Target=%s)\n\t%s\n",
                        $target, $error_msg{$a}));
            }
        }
    }
    if ($type eq "PROC")
    {
        ## note: DMI is checked on membuf side so don't need to check that here
        ## this checks if at least 1 abus is connected
        my $found_abus = 0;
        my $abus_error = "";
        foreach my $child (@{ $targetObj->getTargetChildren($target) })
        {
            my $child_type = $targetObj->getBusType($child);
            if ($child_type eq "ABUS" || $child_type eq "XBUS")
            {
                if ($targetObj->getMasterProc() ne $target)
                {
                    if (!$targetObj->isBadAttribute($child, "PEER_TARGET"))
                    {
                        $found_abus = 1;
                    }
                    else
                    {
                        $abus_error = sprintf(
"proc not connected to proc via Abus or Xbus (Target=%s)",$child);
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
            push(@errors, $abus_error);
        }
    }
    if ($errors[0])
    {
        foreach my $err (@errors)
        {
            print "ERROR: $err\n";
        }
        $targetObj->myExit(3);
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
